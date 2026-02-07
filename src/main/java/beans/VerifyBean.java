package beans;

import db.dbTransactions;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import model.DocumentFile;
import model.ParkingBooking;
import utils.FacesUtil;
import rest.DocumentService;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import model.DocumentSignature;
import model.DocumentFile;

@Named("verifyUiBean")
@ViewScoped
public class VerifyBean implements Serializable {

    private Long docId;

    private Boolean hasResult = false;
    private String rsaOk;
    private String pqcOk;
    private String overall;

    private DocumentService documentService;

    private DocumentSignature signature;
    private DocumentFile document;
    private List<SignatureInfoRow> signatureInfoList;

    private List<DocumentFile> documentFileList = new ArrayList<>();

    public VerifyBean() {
        this.documentService = new DocumentService();
        this.signatureInfoList = new ArrayList<>(); // Αρχικοποίηση για να μην είναι null

    }

    @PostConstruct
    public void init() throws IOException {


        documentFileList = (List<DocumentFile>) (List<?>) dbTransactions.getAllObjects(DocumentFile.class.getCanonicalName());


    }

    public void epilogiArxeiou(DocumentFile documentFile) {

        docId= documentFile.getDocumentId();


        if (FacesContext.getCurrentInstance().getMaximumSeverity() == null) {
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO, "Επιλέξατα το αρχείο  με όνομα ", documentFile.getFilename());
            FacesContext.getCurrentInstance().addMessage(null, msg);
        }

    }

    public void verify() {
        try {
            if (docId == null) {
                FacesMessage facesMessage = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Παρακαλώ προσθέστε τον αριθμό id του εγγράφου", "");
                FacesContext.getCurrentInstance().addMessage("fail", facesMessage);
                return;
            }

            // Καλούμε το DocumentService για επαλήθευση
            DocumentService.VerificationResult result = documentService.verify(docId);

            // Αποθήκευση πληροφοριών υπογραφής και εγγράφου
            this.signature = result.signature;
            this.document = result.document;

            // Μετατρέπουμε Boolean (που μπορεί να είναι null) σε String
            if (result.rsaOk == null) {
                rsaOk = "N/A (χωρίς RSA υπογραφή)";
            } else {
                rsaOk = result.rsaOk ? "OK ✅" : "FAIL ❌";
            }

            if (result.pqcOk == null) {
                pqcOk = "N/A (χωρίς PQC υπογραφή)";
            } else {
                pqcOk = result.pqcOk ? "OK ✅" : "FAIL ❌";
            }

            overall = result.overall ? "OK ✅" : "FAIL ❌";

            // Δημιουργία λίστας με πληροφορίες για τον πίνακα
            buildSignatureInfoList();


            hasResult = true;
//            FacesUtil.info("Verification done for docId=" + docId);
            if (FacesContext.getCurrentInstance().getMaximumSeverity() == null) {
                FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO, "", "To επιλεγμένο αρχείο είναι υπογεγραμμένο");
                FacesContext.getCurrentInstance().addMessage(null, msg);
            }

        } catch (IllegalArgumentException e) {
            hasResult = false;
            signatureInfoList = new ArrayList<>(); // Καθαρισμός λίστας σε περίπτωση σφάλματος
            FacesUtil.error(e.getMessage());
        } catch (Exception e) {
            hasResult = false;
            signatureInfoList = new ArrayList<>(); // Καθαρισμός λίστας σε περίπτωση σφάλματος
            FacesUtil.error("Σφάλμα επαλήθευσης: " + e.getMessage());
            e.printStackTrace(); // για debugging
        }
    }


    private void buildSignatureInfoList() {
        signatureInfoList = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

        // Πληροφορίες Εγγράφου (πάντα προσθέτουμε αν υπάρχει document)
        if (document != null) {
            signatureInfoList.add(new SignatureInfoRow("ID Εγγράφου",
                    document.getDocumentId() != null ? document.getDocumentId().toString() : "N/A"));
            signatureInfoList.add(new SignatureInfoRow("Όνομα Αρχείου",
                    document.getFilename() != null ? document.getFilename() : "N/A"));
            signatureInfoList.add(new SignatureInfoRow("Μέγεθος Αρχείου",
                    document.getFileSize() != null ? document.getFileSize() + " bytes" : "N/A"));
            signatureInfoList.add(new SignatureInfoRow("Ημερομηνία Αποθήκευσης",
                    document.getUploadTime() != null ? sdf.format(document.getUploadTime()) : "N/A"));
        }

        // Πληροφορίες Υπογραφής (αν υπάρχει signature)
        if (signature != null) {
            signatureInfoList.add(new SignatureInfoRow("ID Υπογραφής",
                    signature.getSignatureId() != null ? signature.getSignatureId().toString() : "N/A"));
            signatureInfoList.add(new SignatureInfoRow("Σχήμα Υπογραφής",
                    signature.getScheme() != null ? signature.getScheme() : "N/A"));
            signatureInfoList.add(new SignatureInfoRow("Αλγόριθμος Hash",
                    signature.getHashAlg() != null ? signature.getHashAlg() : "N/A"));
            signatureInfoList.add(new SignatureInfoRow("Ημερομηνία Υπογραφής",
                    signature.getSignTime() != null ? sdf.format(signature.getSignTime()) : "N/A"));

            // Πληροφορίες Υπογραφών (αν υπάρχουν)
            signatureInfoList.add(new SignatureInfoRow("RSA Υπογραφή Υπάρχει",
                    signature.getRsaSignature() != null && signature.getRsaSignature().length > 0
                            ? "Ναι (" + signature.getRsaSignature().length + " bytes)" : "Όχι"));
            signatureInfoList.add(new SignatureInfoRow("PQC Υπογραφή Υπάρχει",
                    signature.getPqcSignature() != null && signature.getPqcSignature().length > 0
                            ? "Ναι (" + signature.getPqcSignature().length + " bytes)" : "Όχι"));
        } else {
            signatureInfoList.add(new SignatureInfoRow("Στάτους Υπογραφής", "Δεν βρέθηκε υπογραφή"));
        }

        // Κατάσταση Επαλήθευσης (πάντα προσθέτουμε)
        signatureInfoList.add(new SignatureInfoRow("RSA Υπογραφή",
                rsaOk != null ? rsaOk : "N/A"));
        signatureInfoList.add(new SignatureInfoRow("PQC Υπογραφή (DILITHIUM)",
                pqcOk != null ? pqcOk : "N/A"));
        signatureInfoList.add(new SignatureInfoRow("Συνολικό Αποτέλεσμα",
                overall != null ? overall : "N/A"));

        // Debug: Εκτύπωση στο console για debugging
        System.out.println("DEBUG: buildSignatureInfoList - signatureInfoList size: " + signatureInfoList.size());
        System.out.println("DEBUG: signature != null: " + (signature != null));
        System.out.println("DEBUG: document != null: " + (document != null));
    }

    // Inner class για τις γραμμές του πίνακα
    public static class SignatureInfoRow implements Serializable {
        private static final long serialVersionUID = 1L;
        private String property;
        private String value;

        public SignatureInfoRow(String property, String value) {
            this.property = property;
            this.value = value;
        }

        public String getProperty() {
            return property;
        }

        public void setProperty(String property) {
            this.property = property;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getValueStyle() {
            if (value != null && (value.contains("OK") || value.contains("FAIL"))) {
                if (value.contains("OK")) {
                    return "color: green; font-weight: bold;";
                } else if (value.contains("FAIL")) {
                    return "color: red; font-weight: bold;";
                }
            }
            return "";
        }
    }
    public boolean isHasResult() {return hasResult;}

    public Long getDocId() {return docId;}

    public void setDocId(Long docId) {
        this.docId = docId;
    }

    public String getRsaOk() {
        return rsaOk;
    }

    public String getPqcOk() {
        return pqcOk;
    }

    public String getOverall() {
        return overall;
    }

    public List<DocumentFile> getDocumentFileList() {return documentFileList;}

    public void setDocumentFileList(List<DocumentFile> documentFileList) {this.documentFileList = documentFileList;}

    public List<SignatureInfoRow> getSignatureInfoList() {
        if (signatureInfoList == null) {
            signatureInfoList = new ArrayList<>();
        }
        return signatureInfoList;
    }
}
