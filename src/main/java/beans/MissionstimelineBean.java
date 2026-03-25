package beans;


import db.HashingUtil;
import db.dbTransactions;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import model.*;
import org.apache.log4j.Logger;
import org.primefaces.PrimeFaces;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Named("missionstimelineBean")
@ViewScoped
public class MissionstimelineBean implements Serializable {


    private Long sinoloAnevasmenwnEggrafwn;
    private Long sinoloIpogegrammenwnEggrafwn;
    private Long sinoloAnipografwnEggrafwn;
    private String currenDate;
    private String currenDateFront;
    private Tbluser tbluser = new Tbluser();
    private String passWord1 = "";
    private String passWord2 = "";
    private String welcomeMsg;

    @Inject
    private LoginBean loginBean;


    private static final Logger log = Logger.getLogger(MissionstimelineBean.class);

    public MissionstimelineBean() {

    }

    @PostConstruct
    public void init() throws IOException {

        Calendar c = Calendar.getInstance();
        int timeOfDay = c.get(Calendar.HOUR_OF_DAY);

        tbluser = loginBean.getUser();



        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        currenDate = LocalDate.now().format(formatter);

        DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        currenDateFront = LocalDate.now().format(formatter1);

        if (timeOfDay >= 5 && timeOfDay < 12) {
            welcomeMsg = "Καλημέρα!";
        } else if (timeOfDay >= 12 && timeOfDay < 17) {
            welcomeMsg = "Καλό Μεσημέρι!";
        } else if (timeOfDay >= 17 && timeOfDay < 20) {
            welcomeMsg = "Καλό Απόγευμα!";
        } else if (timeOfDay >= 20 && timeOfDay < 24) {
            welcomeMsg = "Καλό Βράδυ!";
        } else if (timeOfDay >= 0 && timeOfDay < 5) {
            welcomeMsg = "Δεν είναι καλή ώρα για δουλειά!";
        }

        //Αν είναι καινούριος χρήστης του ανοίγουμε το dialog
        if (loginBean.getNewUserFromGoogle().equals(true)) {

            PrimeFaces current = PrimeFaces.current();
            current.executeScript("PF('dlg3').show();");
        }

        //Αν είναι καινούριος χρήστης του ανοίγουμε το dialog
        if (loginBean.getNewUser().equals(true)) {

            PrimeFaces current = PrimeFaces.current();
            current.executeScript("PF('dlg5').show();");
        }



        //Βρίσκουμε το σύνολο των εγγράφων που υπάρχουν στο σύστημα
        String sinoloAnevasmenwnEggrafwnQuery = "select (e.p)  from (select count(p.DOCUMENT_ID) AS P \n" +
                "from  pqsignatures.document_file p ) e" ;
        sinoloAnevasmenwnEggrafwn = (Long) dbTransactions.getObjectsBySqlQry1(sinoloAnevasmenwnEggrafwnQuery);

        //Βρίσκουμε το σύνολο των εγγράφων που υπάρχουν στο σύστημα
        String sinoloIpogegrammenwnEggrafwnQuery = "select (e.p)  from (select count(p.SIGNATURE_ID) AS P \n" +
                "from  pqsignatures.document_signature p ) e" ;
        sinoloIpogegrammenwnEggrafwn = (Long) dbTransactions.getObjectsBySqlQry1(sinoloIpogegrammenwnEggrafwnQuery);

        sinoloAnipografwnEggrafwn  = sinoloAnevasmenwnEggrafwn - sinoloIpogegrammenwnEggrafwn;

    }


    public void storeObjectsNewUser() {

        if (!passWord1.equals(passWord2)) {
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Ο κωδικός δεν είναι ίδιος και στα 2 πεδία!", "");
            FacesContext.getCurrentInstance().addMessage(null, msg);
            return;
        } else {
            tbluser.setPassword(HashingUtil.SHA1(passWord1));

            try {
                dbTransactions.storeObject(tbluser);

                loginBean.setUser(tbluser);

                FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO, "Επιτυχής αποθήκευση των δεδομένων σας!", "");
                FacesContext.getCurrentInstance().addMessage(null, msg);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void updatePassword() {

        if (!passWord1.equals(passWord2)) {
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Ο κωδικός δεν είναι ίδιος και στα 2 πεδία!", "");
            FacesContext.getCurrentInstance().addMessage(null, msg);
            return;
        } else {
            tbluser.setPassword(HashingUtil.SHA1(passWord1));
            tbluser.setResetPassword(false);
            try {
                dbTransactions.updateObject(tbluser);

                FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO, "Επιτυχής αλλλαγή του Κωδικού!", "");
                FacesContext.getCurrentInstance().addMessage(null, msg);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public LoginBean getLoginBean() {
        return loginBean;
    }

    public void setLoginBean(LoginBean loginBean) {
        this.loginBean = loginBean;
    }

    public Tbluser getTbluser() {
        return tbluser;
    }

    public void setTbluser(Tbluser tbluser) {
        this.tbluser = tbluser;
    }


    public String getPassWord2() {
        return passWord2;
    }

    public void setPassWord2(String passWord2) {
        this.passWord2 = passWord2;
    }

    public String getPassWord1() {
        return passWord1;
    }

    public void setPassWord1(String passWord1) {
        this.passWord1 = passWord1;
    }


    public String getWelcomeMsg() {
        return welcomeMsg;
    }

    public void setWelcomeMsg(String welcomeMsg) {
        this.welcomeMsg = welcomeMsg;
    }


    public Long getSinoloAnevasmenwnEggrafwn() {
        return sinoloAnevasmenwnEggrafwn;
    }

    public void setSinoloAnevasmenwnEggrafwn(Long sinoloAnevasmenwnEggrafwn) {
        this.sinoloAnevasmenwnEggrafwn = sinoloAnevasmenwnEggrafwn;
    }


    public Long getSinoloIpogegrammenwnEggrafwn() {
        return sinoloIpogegrammenwnEggrafwn;
    }

    public void setSinoloIpogegrammenwnEggrafwn(Long sinoloIpogegrammenwnEggrafwn) {
        this.sinoloIpogegrammenwnEggrafwn = sinoloIpogegrammenwnEggrafwn;
    }

    public Long getSinoloAnipografwnEggrafwn() {
        return sinoloAnipografwnEggrafwn;
    }

    public void setSinoloAnipografwnEggrafwn(Long sinoloAnipografwnEggrafwn) {
        this.sinoloAnipografwnEggrafwn = sinoloAnipografwnEggrafwn;
    }
}
