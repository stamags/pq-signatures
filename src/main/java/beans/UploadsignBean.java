package beans;

import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import model.DocumentFile;
import model.DocumentSignature;
import org.primefaces.model.file.UploadedFile;
import rest.DocumentService;
import utils.DocumentAuditService;
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
                FacesUtil.error("Παρακαλώ επιλέξτε ένα αρχείο PDF.");
                return;
            }

            byte[] pdfBytes = uploadedFile.getContent();

            // Υπολογισμός SHA-256 hash
            String sha256 = FileStorageService.calculateSha256(pdfBytes);

            // Αποθήκευση DocumentFile metadata στη βάση δεδομένων (χωρίς δεδομένα αρχείου)
            DocumentFile docNew = new DocumentFile();
            docNew.setFilename(uploadedFile.getFileName());
            docNew.setContentType(uploadedFile.getContentType());
            docNew.setFileSize((long) pdfBytes.length);
            docNew.setSha256(sha256);

            dbTransactions.storeObject(docNew);
            lastDocumentId = docNew.getDocumentId();

            //Αποθήκευση αρχείου στο filesystem
            String storagePath = FileStorageService.saveFile(lastDocumentId, pdfBytes);
            docNew.setStoragePath(storagePath);
            dbTransactions.storeWithMergeObject(docNew); // Ενημέρωση με διαδρομή αποθήκευσης

            //Επαναφόρτωση DocumentFile από τη βάση δεδομένων για να είναι managed (όχι detached)
            DocumentFile managedDoc = dbTransactions.getObjectById(DocumentFile.class, lastDocumentId);

            // Υπογραφή
            DocumentSignature sig =
                    DocumentService.signDocument(managedDoc, scheme);

            dbTransactions.storeObject(sig);
            lastSignatureId = sig.getSignatureId();


            DocumentAuditService.recordEvent(lastDocumentId, DocumentAuditService.ACTION_UPLOAD, DocumentAuditService.STATUS_SUCCESS);
            DocumentAuditService.recordEvent(lastDocumentId, DocumentAuditService.ACTION_SIGN, DocumentAuditService.STATUS_SUCCESS);
            FacesUtil.info("Upload & Sign ολοκληρώθηκε. docId=" + lastDocumentId + ", sigId=" + lastSignatureId);

        } catch (Exception e) {
            if (lastDocumentId != null) {
                DocumentAuditService.recordEvent(lastDocumentId, DocumentAuditService.ACTION_SIGN, DocumentAuditService.STATUS_FAILURE);
            }
            FacesUtil.error("Σφάλμα: " + e.getMessage());
        }
    }

    // getters/setters
    public UploadedFile getUploadedFile() {
        return uploadedFile;
    }

    public void setUploadedFile(UploadedFile uploadedFile) {
        this.uploadedFile = uploadedFile;
    }

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public Long getLastDocumentId() {
        return lastDocumentId;
    }

    public Long getLastSignatureId() {
        return lastSignatureId;
    }
}