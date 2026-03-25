/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package beans;

import db.dbTransactions;
import model.*;


import jakarta.annotation.PostConstruct;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;


/**
 * @author Stamags
 */

@Named("OloiOiXristesBean")
@ViewScoped
public class OloiOiXristesBean implements Serializable {


    private List<Tbluser> tbluserList = new ArrayList<>();



    @Inject
    private LoginBean loginBean;

    @PostConstruct
    public void init() throws IOException {

        tbluserList = (List<Tbluser>) (List<?>) dbTransactions.getAllObjects(Tbluser.class.getCanonicalName());

    }


    public void deactivateUser(Tbluser tbluser) {

        tbluser.setUserActive(0);

        dbTransactions.updateObject(tbluser);

        if (FacesContext.getCurrentInstance().getMaximumSeverity() == null) {
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO, "Επιτυχής Απενεργοποίση Χρήστη!", "");
            FacesContext.getCurrentInstance().addMessage(null, msg);
        }
    }

    public void activαteUser(Tbluser tbluser) {

        tbluser.setUserActive(1);

        dbTransactions.updateObject(tbluser);

        if (FacesContext.getCurrentInstance().getMaximumSeverity() == null) {
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO, "Επιτυχής Ενεργοποίηση Χρήστη!", "");
            FacesContext.getCurrentInstance().addMessage(null, msg);
        }
    }

    public void resetPassword(Tbluser tbluser) {

        tbluser.setResetPassword(true);

        dbTransactions.updateObject(tbluser);

        if (FacesContext.getCurrentInstance().getMaximumSeverity() == null) {
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO, "Ο χρήστης στην επόμενη είσοδό του θα πρέπει να αλλάξει τον κωδικό του!", "");
            FacesContext.getCurrentInstance().addMessage(null, msg);
        }

    }

    public List<Tbluser> getTbluserList() {
        return tbluserList;
    }

    public void setTbluserList(List<Tbluser> tbluserList) {
        this.tbluserList = tbluserList;
    }

    public LoginBean getLoginBean() {
        return loginBean;
    }

    public void setLoginBean(LoginBean loginBean) {
        this.loginBean = loginBean;
    }


}

