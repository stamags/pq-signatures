package beans;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import db.HashingUtil;

import java.io.*;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import db.dbTransactions;
import manhattan.GuestPreferences;
import model.*;
import org.apache.commons.lang3.SystemUtils;
import org.apache.log4j.Logger;
import security.Security;
import utils.GoogleTokenVerifierUtil;

/**
 * Αυθεντικοποιεί τους χρήστες και ανεβάζει στο session το αντικείμενο
 * <code>Monimos</code> για τον συνδεδεμένο χρήστη.
 */

@Named("LoginBean")
@SessionScoped
public class LoginBean implements Serializable {

    public static final String AUTH_KEY = "username_auth";
    private String idToken = null;  // Property to hold the token from the front end
    private Boolean newUserFromGoogle = false;
    private Boolean newUser = false;
    private Boolean isGuest = false;
    private Tbluser tbluser = new Tbluser();


    private Integer currentYear;

    //variables
    private boolean changePwd = false;
    private String username = "";
    private String password = "";
    private Security security;
    //Lists
    private List<Tbluser> usersList;
    //Oblects
    private Tbluser user;
    //Είναι μεταβλητές για να πάρουμε απο το property αρχείο τα δεδομένα.
    public static String currentEsso = "";
    private String googleClientId = "1041078813908-78r2hcsv75bitv7t0o5anbci4lkfthqk.apps.googleusercontent.com";
    public static Long currentMonada;
    public static String remoteServer = "";


    private static final Logger log = Logger.getLogger(LoginBean.class.getCanonicalName());

    @Inject
    private GuestPreferences guestPreferencesBean;


    public LoginBean() {
    }

    public static Logger getLog() {
        return log;
    }


    @PostConstruct
    public void init() {
        newUser = false;
        isGuest= false;
        try {
            //Ανακτά το όνομα της βάσης
            HttpSession session = (HttpSession) FacesContext.getCurrentInstance().getExternalContext().getSession(true);
            currentYear = Calendar.getInstance().get(Calendar.YEAR);

            guestPreferencesBean = new GuestPreferences();
            guestPreferencesBean.setTheme("green-pink");
        } catch (Exception ex) {
            ex.printStackTrace();
        }


    }

    /**
     * Processes the Google ID token received from the front end.
     */
    public void processGoogleToken() {
        FacesContext fc = FacesContext.getCurrentInstance();
        HttpServletRequest req = (HttpServletRequest) fc.getExternalContext().getRequest();
//        String idTokenString = req.getParameter("idtoken");

        // For debugging: print out the token received.
        System.out.println("Processing Google token: " + idToken);

//        if (idTokenString == null || idTokenString.isEmpty()) {
//            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Login Error", "ID token is missing"));
//            return;
//        }

        //Η περίπτωση που θα κάνουμε Login με username και password

        // Verify the token using our utility method.
        GoogleIdToken.Payload payload = GoogleTokenVerifierUtil.verifyToken(idToken);

        if (payload != null) {
            // Token is valid. Extract user details.
            String email = payload.getEmail();
            String name = (String) payload.get("name");
            // You might also extract other information such as picture URL, locale, etc.
            System.out.println("Google token verified. Email: " + email + " Name: " + name);

            // Optionally, look up the user in your database using the email.
            List<Tbluser> matchingUsers = (List<Tbluser>) (List<?>) dbTransactions.getObjectsByProperty(
                    Tbluser.class.getCanonicalName(), "email", email);
            if (matchingUsers != null && !matchingUsers.isEmpty()) {
                newUser = false;
                user = matchingUsers.get(0);
                // Set session attribute
                req.getSession().setAttribute(AUTH_KEY, user.getUsername());
                // You might initialize security and guest preferences here.
                security = new Security(user);
                // Redirect or update UI as needed.
                try {
                    fc.getExternalContext().redirect("webContent/missionstimeline.jsf?faces-redirect=true");
                } catch (IOException e) {
                    log.error(e.getLocalizedMessage());
                }
            } else {
                //Αυτό το κάνω γιατί ο tbluser συνδέεται με τον tblemployee
//                List<Tblemployee> tblemployeeList = (List<Tblemployee>) (List<?>) dbTransactions.getAllObjects(Tblemployee.class.getCanonicalName());
                //Αυτό το κάνω γιατί ο tbluser συνδέεται με τον tblemployee
                List<Tblroles> tblrolesList = (List<Tblroles>) (List<?>) dbTransactions.getAllObjects(Tblroles.class.getCanonicalName());
                //Αν μπαίνει πρώτη φορά με google account τον δημιουργώ νέο χρήστη
                Tbluser tbluser = new Tbluser();
                tbluser.setName(payload.get("given_name").toString());
                tbluser.setSurname(payload.get("family_name").toString());
                tbluser.setUsername(payload.get("email").toString());
                tbluser.setEmail(payload.get("email").toString());
                tbluser.setTheme("blue-grey");
                tbluser.setResetPassword(false);
                tbluser.setLayoutMode("static");
                tbluser.setLightMenu(false);
                tbluser.setUserActive(1);
                tbluser.setPassword("123456");
                tbluser.setAvatar(payload.get("picture").toString());
                tbluser.setIdRole(tblrolesList.get(1));
                newUserFromGoogle = true;

                try {
                    dbTransactions.storeObject(tbluser);
                    //To boolean το χρησιμοποιούμε για να βγάλουμε μήνυμα στο missionstimeline ότι είναι καινούριος ο χρήστης και έκανε εγγραφή!
                    newUserFromGoogle = true;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                user = tbluser;
                req.getSession().setAttribute(AUTH_KEY, user.getUsername());
                // You might initialize security and guest preferences here.
                security = new Security(user);
                // Redirect or update UI as needed.
                try {
                    fc.getExternalContext().redirect("webContent/missionstimeline.jsf?faces-redirect=true");
                } catch (IOException e) {
                    log.error(e.getLocalizedMessage());
                }
            }
        } else {
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Login Error", "Invalid Google token"));
        }

    }


    /**
     * Επιστρέφει το όνομα του theme του χρήστη.
     *
     * @return
     */
    public String getTheme() {
        //Default theme
        String theme = "blue-grey";

        if (user != null) {
            if (user.getTheme() != null) {
                theme = user.getTheme();
            }
        }
        return theme;

    }


    public String validateUser() {

        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        FacesMessage message = new FacesMessage();

        if (username.isEmpty() || username == "" || username == null) {
            message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Wrong login", "You must insert a username ");

//            RequestContext.getCurrentInstance().showMessageInDialog(message);
            return null;
        } else if (password.isEmpty() || password == "") {
            message = new FacesMessage(FacesMessage.SEVERITY_INFO, "Wrong login", "You must insert a password ");
//            RequestContext.getCurrentInstance().showMessageInDialog(message);
            return null;
        } else {


            // Αναζήτηση για το χρήστη
            setUsersList((List<Tbluser>) (List<?>) db.dbTransactions.getObjectsByProperty(model.Tbluser.class.getCanonicalName(), "username", getUsername()));
//            System.out.println("usersList.size()------------------->" + usersList.size());
            // Αν είναι άδεια η λίστα, επιστροφή
            if (getUsersList().isEmpty()) {
                String failMessage = username + ":" + password + " failed";
                request.getSession().setAttribute(AUTH_KEY, null);
                message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Wrong Login", "Inserted credentials are wrong");
//                RequestContext.getCurrentInstance().showMessageInDialog(message);
                return "null";
            }

            // Αν είναι σωστός ο συνδυασμός username/password, εισαγωγή στην εφαρμογή
            user = getUsersList().get(0);

            //Θα ελέγξουμε αρχικά αν είναι ενεργός ο χρήστης
            if (user.getUserActive() == 1) {
                if (getUsername().equals(user.getUsername()) && HashingUtil.SHA1(getPassword()).equals(user.getPassword())) {
                    request.getSession().setAttribute(AUTH_KEY, getUsername());

                    //Φτιάχνουμε το security
                    security = new Security(user);

                    //Βάζουμε το theme που έχει eπιλέξει ο χρήστης
                    guestPreferencesBean = new GuestPreferences();

                    if (user.getLightMenu() != null && user.getLightMenu()) {
                        guestPreferencesBean.setLightMenu(true);
                    } else {
                        guestPreferencesBean.setLightMenu(false);
                    }


                    //TODO:Takis Για να ενεργοποιήσουμε την αλλαγή κωδικού πρέπει να αλλάξουμε το 1234567 σε 123456
                    String defaultPassword = HashingUtil.SHA1("123456");
                    if (user.getPassword().equals(defaultPassword)) {
                        return "/webContent/changePassword.jsf?faces-redirect=true";
                    } else {
                        return "webContent/missionstimeline.jsf?faces-redirect=true";
                    }
                } else {
                    String failMessage = username + ":" + password + " failed";
                    request.getSession().setAttribute(AUTH_KEY, null);
                    message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Wrong Login", "Inserted credentials are wrong");
//                RequestContext.getCurrentInstance().showMessageInDialog(message);
                    return null;
                    // return "/login.jsf";
                }

            } else {
                FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Δυστυχώς ο χρήστης που προσπαθείτε να μπείτε είναι ανενεργός!", "");
                FacesContext.getCurrentInstance().addMessage(null, msg);
                return null;
            }
        }
    }


    public String loginAsGuest() {
        FacesContext fc = FacesContext.getCurrentInstance();
//        try {


        //Αυτό το κάνω γιατί ο tbluser συνδέεται με τον tblemployee
//        List<Tblemployee> tblemployeeList = (List<Tblemployee>) (List<?>) dbTransactions.getAllObjects(Tblemployee.class.getCanonicalName());
        //Αυτό το κάνω γιατί ο tbluser συνδέεται με τον tblemployee
        List<Tblroles> tblrolesList = (List<Tblroles>) (List<?>) dbTransactions.getAllObjects(Tblroles.class.getCanonicalName());

        // Δημιουργούμε placeholder χρήστη
        tbluser = new Tbluser();
        tbluser.setIdUser(5);
        tbluser.setName("Επισκέπτης");
        tbluser.setSurname("-");
        tbluser.setEmail("guest@guest.local");
        tbluser.setUsername("guest@guest.local");
        tbluser.setTheme("blue-grey");
        tbluser.setResetPassword(false);
        tbluser.setLayoutMode("static");
        tbluser.setLightMenu(false);
        tbluser.setUserActive(1);
        tbluser.setIdRole(tblrolesList.get(2));// τον βάζω ως guest
        user = tbluser;
        username = tbluser.getUsername();
        //Φτιάχνουμε το security
        security = new Security(tbluser);
        isGuest = true;
        //Βάζουμε το theme που έχει eπιλέξει ο χρήστης
        guestPreferencesBean = new GuestPreferences();

        if (tbluser.getLightMenu() != null && tbluser.getLightMenu()) {
            guestPreferencesBean.setLightMenu(true);
        } else {
            guestPreferencesBean.setLightMenu(false);
        }
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        request.getSession().setAttribute(AUTH_KEY, getUsername());
        return "webContent/missionstimeline.jsf?faces-redirect=true";
    }


    public String newUserLogin() {

        //Αυτό το κάνω γιατί ο tbluser συνδέεται με τον tblemployee
//        List<Tblemployee> tblemployeeList = (List<Tblemployee>) (List<?>) dbTransactions.getAllObjects(Tblemployee.class.getCanonicalName());
        //Αυτό το κάνω γιατί ο tbluser συνδέεται με τον tblemployee
        List<Tblroles> tblrolesList = (List<Tblroles>) (List<?>) dbTransactions.getAllObjects(Tblroles.class.getCanonicalName());

        // Δημιουργούμε placeholder χρήστη
        tbluser = new Tbluser();
//        tbluser.setIdUser(5);
//        tbluser.setName();
//        tbluser.setSurname();
//        tbluser.setEmail();
        tbluser.setTheme("blue-grey");
        tbluser.setResetPassword(false);
        tbluser.setLayoutMode("static");
        tbluser.setLightMenu(false);
        tbluser.setUserActive(1);
        tbluser.setIdRole(tblrolesList.get(1));// τον βάζω ως guest
        user = tbluser;
        username = "-";

        newUser = true;
        //Φτιάχνουμε το security
        security = new Security(tbluser);

        //Βάζουμε το theme που έχει eπιλέξει ο χρήστης
        guestPreferencesBean = new GuestPreferences();

        if (tbluser.getLightMenu() != null && tbluser.getLightMenu()) {
            guestPreferencesBean.setLightMenu(true);
        } else {
            guestPreferencesBean.setLightMenu(false);
        }
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        request.getSession().setAttribute(AUTH_KEY, getUsername());
        return "webContent/missionstimeline.jsf?faces-redirect=true";
    }


    public Boolean isAdmin() {
        Boolean isAdmin = false;
        isAdmin = user.getIdRole().getRoleDesc().equals("Administrator");
        return isAdmin;
    }


    /**
     * call redirect function in class Security
     * Άμα βρίσκει την σελίδα θα πρέπει να δίνει πρόσβαση στον χρήστη
     *
     * @param pageName
     */
    public boolean redirect(String pageName) {
//        log.info("pageName -->" + pageName);
        boolean hasAccessToPage = getSecurity() != null && getSecurity().foundPageName(pageName);
        if (!hasAccessToPage) {
            try {
                FacesContext.getCurrentInstance().getExternalContext().redirect("/pearl/webContent/missionstimeline.jsf?faces-redirect=true");
            } catch (IOException ex) {
                log.error(ex.getLocalizedMessage());
            }
        }
        return hasAccessToPage;
    }


//    /**
//     * Επιστρέφει to avatar για τον χρήστη.
//     * Άμα ο χρήστης έχει ανεβάσει κάποιο avatar τότε θα του επιστρέψει αυτό
//     * Άμα δεν έχει ανεβάσει θα του επιστρέψει ένα dummy ανάλογα με το φύλο του
//     *
//     * @param useravatar
//     * @return
//     * @throws IOException
//     */
//    public String avatarGender(Tbluser useravatar) throws IOException {
//        // Έχουμε μετατρέψει τις φωτοφγραφίες του dummy avatar του χρήστη σε base64 images.
//        // Λόγο όμως του ότι ήταν πολύ μαγάλες και χωρούσε σε ένα string τις βάλαμε σε ένα properties αρχείο απο όπου
//        // πηγαίνει και τα διαβάζει απο εκεί.
//
//        Properties prop = new Properties();
//        prop.load(getClass().getResourceAsStream("/gender.properties"));
//
//        if (useravatar.getPassword().equals("bot")) {
//            return prop.getProperty("bot");
//        }
//
//
//    }


    /**
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @param username the username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * @return the user
     */
    public Tbluser getUser() {
        return user;
    }

    /**
     * @param user the user to set
     */
    public void setUser(Tbluser user) {
        this.user = user;
    }

    /**
     * @return the changePwd
     */
    public boolean isChangePwd() {
        return changePwd;
    }

    /**
     * @param changePwd the changePwd to set
     */
    public void setChangePwd(boolean changePwd) {
        this.changePwd = changePwd;
    }

    /**
     * @return the usersList
     */
    public List<Tbluser> getUsersList() {
        return usersList;
    }

    /**
     * @param usersList the usersList to set
     */
    public void setUsersList(List<Tbluser> usersList) {
        this.usersList = usersList;
    }

    public Integer getCurrentYear() {
        return currentYear;
    }

    public void setCurrentYear(Integer currentYear) {
        this.currentYear = currentYear;
    }

    public Security getSecurity() {
        return security;
    }

    public void setSecurity(Security security) {
        this.security = security;
    }


    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }

    public Boolean getNewUser() {
        return newUser;
    }

    public void setNewUser(Boolean newUser) {
        this.newUser = newUser;
    }

    public Boolean getNewUserFromGoogle() {
        return newUserFromGoogle;
    }

    public void setNewUserFromGoogle(Boolean newUserFromGoogle) {
        this.newUserFromGoogle = newUserFromGoogle;
    }

    public Boolean getGuest() {
        return isGuest;
    }

    public void setGuest(Boolean guest) {
        isGuest = guest;
    }
}