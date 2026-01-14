package beans;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import utils.FacesUtil;
import rest.DocumentService;

import java.io.Serializable;

@Named("verifyUiBean")
@ViewScoped
public class VerifyBean implements Serializable {

    private Long docId;

    private boolean hasResult;
    private String rsaOk;
    private String pqcOk;
    private String overall;

    private DocumentService documentService;

    public VerifyBean() {
        this.documentService = new DocumentService();
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

            // Μετατρέπουμε Boolean (που μπορεί να είναι null) σε String
            if (result.rsaOk == null) {
                rsaOk = "N/A (no RSA signature)";
            } else {
                rsaOk = result.rsaOk ? "OK ✅" : "FAIL ❌";
            }

            if (result.pqcOk == null) {
                pqcOk = "N/A (no PQC signature)";
            } else {
                pqcOk = result.pqcOk ? "OK ✅" : "FAIL ❌";
            }

//            rsaOk = rsa ? "OK" : "FAIL";
//            pqcOk = pqc ? "OK" : "FAIL";
//            overall = (rsa && pqc) ? "OK" : "FAIL";
            overall = result.overall ? "OK ✅" : "FAIL ❌";

            hasResult = true;
//            FacesUtil.info("Verification done for docId=" + docId);
            FacesUtil.info("Verification completed for docId=" + docId);

        } catch (IllegalArgumentException e) {
            hasResult = false;
            FacesUtil.error(e.getMessage());
        } catch (Exception e) {
            hasResult = false;
            FacesUtil.error("Verification error: " + e.getMessage());
            e.printStackTrace(); // για debugging
        }
    }

    public boolean isHasResult() {
        return hasResult;
    }

    public Long getDocId() {
        return docId;
    }

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
}
