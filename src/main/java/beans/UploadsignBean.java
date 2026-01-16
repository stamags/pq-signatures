package beans;

import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import model.DocumentFile;
import model.DocumentSignature;
import org.primefaces.model.file.UploadedFile;
import rest.DocumentService;
import utils.FacesUtil;
import utils.FileStorageService;
import db.dbTransactions;

import java.io.Serializable;

@Named
@ViewScoped
public class UploadsignBean implements Serializable {

    private UploadedFile uploadedFile;
    private String scheme = "HYBRID";

    private Long lastDocumentId;
    private Long lastSignatureId;

    // TODO: inject your dbtransactions/service here
    // @Inject private DocumentService documentService;

    public void uploadAndSign() {
        try {
            if (uploadedFile == null || uploadedFile.getContent() == null || uploadedFile.getContent().length == 0) {
                FacesUtil.error("Please select a PDF file.");
                return;
            }

            byte[] pdfBytes = uploadedFile.getContent();

            // 1️⃣ Calculate SHA-256 hash
            String sha256 = FileStorageService.calculateSha256(pdfBytes);

            // 2️⃣ Save DocumentFile metadata to DB (without file data)
            DocumentFile doc = new DocumentFile();
            doc.setFilename(uploadedFile.getFileName());
            doc.setContentType(uploadedFile.getContentType());
            doc.setFileSize((long) pdfBytes.length);
            doc.setSha256(sha256);

            dbTransactions.storeObject(doc);
            lastDocumentId = doc.getDocumentId();

            // 3️⃣ Save file to filesystem
            String storagePath = FileStorageService.saveFile(lastDocumentId, pdfBytes);
            doc.setStoragePath(storagePath);
            dbTransactions.storeWithMergeObject(doc); // Update with storage path

            // 4️⃣ Reload DocumentFile from DB to ensure it's managed (not detached)
            DocumentFile managedDoc = dbTransactions.getObjectById(DocumentFile.class, lastDocumentId);

            // 5️⃣ Υπογραφή
            DocumentSignature sig =
                    DocumentService.signDocument(managedDoc, scheme);

            dbTransactions.storeObject(sig);
            lastSignatureId = sig.getSignatureId();


            FacesUtil.info("Upload & Sign completed. docId=" + lastDocumentId + ", sigId=" + lastSignatureId);

        } catch (Exception e) {
            FacesUtil.error("Error: " + e.getMessage());
        }
    }

    // getters/setters
    public UploadedFile getUploadedFile() { return uploadedFile; }
    public void setUploadedFile(UploadedFile uploadedFile) { this.uploadedFile = uploadedFile; }

    public String getScheme() { return scheme; }
    public void setScheme(String scheme) { this.scheme = scheme; }

    public Long getLastDocumentId() { return lastDocumentId; }
    public Long getLastSignatureId() { return lastSignatureId; }
}