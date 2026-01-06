package beans;

import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import utils.FacesUtil;

import java.io.Serializable;

@Named("verifyUiBean")
@ViewScoped
public class VerifyBean implements Serializable {

    private Long docId;

    private boolean hasResult;
    private String rsaOk;
    private String pqcOk;
    private String overall;


    public void verify() {
        try {
            if (docId == null) {
                FacesUtil.error("Please provide a Document ID.");
                return;
            }

            // VerificationResult res = documentService.verify(docId);

            // προσωρινά:
            boolean rsa = true;
            boolean pqc = true;

            rsaOk = rsa ? "OK" : "FAIL";
            pqcOk = pqc ? "OK" : "FAIL";
            overall = (rsa && pqc) ? "OK" : "FAIL";

            hasResult = true;
            FacesUtil.info("Verification done for docId=" + docId);

        } catch (Exception e) {
            FacesUtil.error("Error: " + e.getMessage());
        }
    }

    public boolean isHasResult() { return hasResult; }

    public Long getDocId() { return docId; }
    public void setDocId(Long docId) { this.docId = docId; }

    public String getRsaOk() { return rsaOk; }
    public String getPqcOk() { return pqcOk; }
    public String getOverall() { return overall; }
}
