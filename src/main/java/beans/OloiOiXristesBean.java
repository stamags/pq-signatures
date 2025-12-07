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
    private Map<Integer, ParkingUsersCar> parkingUsersCarMap = new HashMap<>();
    private List<ParkingUsersCar> parkingUsersCarList = new ArrayList<>();


    @Inject
    private LoginBean loginBean;

    @PostConstruct
    public void init() throws IOException {
        parkingUsersCarList = (List<ParkingUsersCar>) (List<?>) dbTransactions.getAllObjects(ParkingUsersCar.class.getCanonicalName());
        tbluserList = (List<Tbluser>) (List<?>) dbTransactions.getAllObjects(Tbluser.class.getCanonicalName());

        //Φτιάχνουμε τα maps.
        for (int i = 0; i < tbluserList.size(); i++) {
            int finalI = i;
           List<ParkingUsersCar> parkingUsersCarList1 = parkingUsersCarList.stream().filter(x -> x.getIdUser().getIdUser().equals(tbluserList.get(finalI).getIdUser())).collect(Collectors.toList());
            if (!Objects.isNull(parkingUsersCarList1) && parkingUsersCarList1.size() > 0) {
                parkingUsersCarMap.put(tbluserList.get(finalI).getIdUser(), parkingUsersCarList1.get(0));
            }
        }
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

    public Map<Integer, ParkingUsersCar> getParkingUsersCarMap() {
        return parkingUsersCarMap;
    }

}

