package beans;

import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import model.DocumentFile;
import model.DocumentSignature;
import model.Tbluser;
import org.primefaces.model.file.UploadedFile;
import rest.DocumentService;
import utils.DocumentAuditService;
import utils.FacesUtil;
import utils.FileStorageService;
import utils.UserKeystoreService;
import db.dbTransactions;

import java.io.Serializable;

@Named("uploadsignBean")
@ViewScoped
public class UploadsignBean implements Serializable {

    private UploadedFile uploadedFile;
    private String scheme = "HYBRID";

    private Long lastDocumentId;
    private Long lastSignatureId;

    @Inject
    private LoginBean loginBean;

    public String uploadAndSign() {
        Tbluser user = loginBean != null ? loginBean.getUser() : null;
        if (user == null) {
            FacesUtil.error("Δεν είστε συνδεδεμένος.");
            return null;
        }
        if (!UserKeystoreService.hasUserKeystore(user.getUsername())) {
            FacesUtil.error("Πρέπει πρώτα να δημιουργήσετε τα προσωπικά σας κλειδιά υπογραφής. Μετάβαση στη σελίδα «Προσωπικά κλειδιά».");
            return "/webContent/user-keys?faces-redirect=true";
        }
        if (loginBean.getKeystorePassword() == null || loginBean.getKeystorePassword().length == 0) {
            FacesUtil.error("Πρέπει να εισάγετε τον κωδικό keystore σας. Μετάβαση στη σελίδα «Προσωπικά κλειδιά» για να ξεκλειδώσετε.");
            return "/webContent/user-keys?faces-redirect=true";
        }
        try {
            if (uploadedFile == null || uploadedFile.getContent() == null || uploadedFile.getContent().length == 0) {
                FacesUtil.error("Παρακαλώ επιλέξτε ένα αρχείο PDF.");
                return null;
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

            // Υπογραφή με κλειδιά του τρέχοντα χρήστη
            DocumentSignature sig = DocumentService.signDocument(
                    managedDoc, scheme, loginBean.getUser(), loginBean.getKeystorePassword());

            dbTransactions.storeObject(sig);
            lastSignatureId = sig.getSignatureId();


            DocumentAuditService.recordEvent(lastDocumentId, DocumentAuditService.ACTION_UPLOAD, DocumentAuditService.STATUS_SUCCESS);
            DocumentAuditService.recordEvent(lastDocumentId, DocumentAuditService.ACTION_SIGN, DocumentAuditService.STATUS_SUCCESS);
            FacesUtil.info("Upload & Sign ολοκληρώθηκε. docId=" + lastDocumentId + ", sigId=" + lastSignatureId);
            return null;

        } catch (Exception e) {
            if (lastDocumentId != null) {
                DocumentAuditService.recordEvent(lastDocumentId, DocumentAuditService.ACTION_SIGN, DocumentAuditService.STATUS_FAILURE);
            }
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("password") || msg.contains("keystore") || msg.contains("key") || msg.contains("no such file") || msg.contains("not found")) {
                FacesUtil.error("Σφάλμα κλειδιών ή κωδικού. Δοκιμάστε ξανά από «Προσωπικά κλειδιά» να ξεκλειδώσετε με τον κωδικό keystore, ή να δημιουργήσετε νέα κλειδιά αν ξεχάσατε τον κωδικό.");
            } else {
                FacesUtil.error("Σφάλμα: " + e.getMessage());
            }
            return null;
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