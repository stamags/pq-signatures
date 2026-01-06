package beans;

import jakarta.enterprise.context.ViewScoped;
import jakarta.inject.Named;
import org.primefaces.model.file.UploadedFile;

import java.io.Serializable;

@Named
@ViewScoped
public class DocumentUiBean implements Serializable {

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
            String filename = uploadedFile.getFileName();
            String contentType = uploadedFile.getContentType();

            // 1) Save PDF to DB -> returns documentId
            // lastDocumentId = documentService.saveDocument(filename, contentType, pdfBytes);

            // 2) Sign -> returns signatureId
            // lastSignatureId = documentService.signDocument(lastDocumentId, scheme);

            // προσωρινά για να μην σπάει compile:
            lastDocumentId = 1L;
            lastSignatureId = 1L;

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