package beans;

import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import model.Tbluser;
import signatures.KeyLoader;
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
    /** Κωδικός για ξεκλείδωμα keystore στην τρέχουσα συνεδρία (όταν έχει ήδη κλειδιά). */
    private String sessionUnlockPassword = "";

    @Inject
    private LoginBean loginBean;

    public boolean isHasKeys() {
        Tbluser u = getCurrentUser();
        return u != null && UserKeystoreService.hasUserKeystore(u.getUsername());
    }

    /** Απόλυτη διαδρομή του φακέλου κλειδιών (για εμφάνιση στη σελίδα). */
    public String getKeysFolderPath() {
        Tbluser u = getCurrentUser();
        if (u == null) return "";
        return UserKeystoreService.getUserKeysDir(u.getUsername()).toAbsolutePath().toString();
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

    /**
     * Ξεκλείδωμα keystore για την τρέχουσα συνεδρία (εισάγετε τον κωδικό που δώσατε κατά τη δημιουργία κλειδιών).
     */
    public String unlockForSession() {
        Tbluser u = getCurrentUser();
        if (u == null) {
            FacesUtil.error("Δεν είστε συνδεδεμένος.");
            return null;
        }
        if (sessionUnlockPassword == null || sessionUnlockPassword.isEmpty()) {
            FacesUtil.error("Εισάγετε τον κωδικό keystore σας.");
            return null;
        }
        try {
            KeyLoader.loadRsaPrivateKeyForUser(u.getUsername(), sessionUnlockPassword.toCharArray());
            loginBean.setKeystorePassword(sessionUnlockPassword.toCharArray());
            sessionUnlockPassword = "";
            FacesUtil.info("Ξεκλειδώσατε τα κλειδιά σας. Μπορείτε πλέον να υπογράφετε.");
            return null;
        } catch (Exception e) {
            FacesUtil.error("Λάθος κωδικός. Δοκιμάστε ξανά.");
            return null;
        }
    }

    /**
     * Ξέχασα τον κωδικό: διαγραφή κλειδιών ώστε ο χρήστης να μπορεί να δημιουργήσει νέα.
     * Ο κωδικός keystore δεν ανακτάται. Οι παλιές υπογραφές δεν θα επαληθεύονται.
     */
    public String resetKeysForgotPassword() {
        Tbluser u = getCurrentUser();
        if (u == null) {
            FacesUtil.error("Δεν είστε συνδεδεμένοι.");
            return null;
        }
        try {
            UserKeystoreService.deleteUserKeystore(u.getUsername());
            if (loginBean != null) {
                loginBean.setKeystorePassword(null);
            }
            sessionUnlockPassword = "";
            FacesUtil.info("Τα παλιά κλειδιά διαγράφηκαν. Μπορείτε να δημιουργήσετε νέα κλειδιά παρακάτω.");
            return null;
        } catch (Exception e) {
            FacesUtil.error("Σφάλμα διαγραφής κλειδιών: " + e.getMessage());
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

    public String getSessionUnlockPassword() {
        return sessionUnlockPassword;
    }

    public void setSessionUnlockPassword(String sessionUnlockPassword) {
        this.sessionUnlockPassword = sessionUnlockPassword;
    }
}
