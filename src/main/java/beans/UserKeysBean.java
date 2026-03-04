package beans;

import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import model.Tbluser;
import utils.FacesUtil;
import utils.UserKeystoreService;

import java.io.Serializable;

/**
 * Bean για τη σελίδα δημιουργίας προσωπικών κλειδιών υπογραφής (RSA + PQC) ανά χρήστη.
 * Ο κωδικός keystore αποθηκεύεται στο session (LoginBean) ώστε να χρησιμοποιείται για υπογραφή.
 */
@Named("userKeysBean")
@ViewScoped
public class UserKeysBean implements Serializable {

    private String password = "";
    private String confirmPassword = "";

    @Inject
    private LoginBean loginBean;

    public boolean isHasKeys() {
        Tbluser u = getCurrentUser();
        return u != null && UserKeystoreService.hasUserKeystore(u.getUsername());
    }

    public String createKeys() {
        Tbluser u = getCurrentUser();
        if (u == null) {
            FacesUtil.error("Δεν είστε συνδεδεμένος.");
            return null;
        }
        if (password == null || password.length() < 4) {
            FacesUtil.error("Ο κωδικός πρέπει να έχει τουλάχιστον 4 χαρακτήρες.");
            return null;
        }
        if (!password.equals(confirmPassword)) {
            FacesUtil.error("Ο κωδικός και η επιβεβαίωση δεν ταιριάζουν.");
            return null;
        }
        try {
            UserKeystoreService.createKeystoreForUser(u, password.toCharArray());
            loginBean.setKeystorePassword(password.toCharArray());
            password = "";
            confirmPassword = "";
            FacesUtil.info("Τα προσωπικά σας κλειδιά δημιουργήθηκαν. Μπορείτε πλέον να υπογράφετε.");
            return null;
        } catch (Exception e) {
            FacesUtil.error("Σφάλμα δημιουργίας κλειδιών: " + e.getMessage());
            return null;
        }
    }

    private Tbluser getCurrentUser() {
        return loginBean != null ? loginBean.getUser() : null;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public LoginBean getLoginBean() {
        return loginBean;
    }

    public void setLoginBean(LoginBean loginBean) {
        this.loginBean = loginBean;
    }
}
