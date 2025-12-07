/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package beans;

import db.dbTransactions;
import model.*;
import org.apache.commons.lang3.StringUtils;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Stamags
 */

@Named("OlesOiKratiseisBean")
@ViewScoped
public class OlesOiKratiseisBean implements Serializable {

    private Tbluser tbluser = new Tbluser();
    private List<ParkingBooking> parkingBookingList = new ArrayList<>();



    @Inject
    private LoginBean loginBean;

    @PostConstruct
    public void init() throws IOException {

        parkingBookingList = (List<ParkingBooking>) (List<?>) dbTransactions.getAllObjects(ParkingBooking.class.getCanonicalName());


   }

    public void deleteKratisi(ParkingBooking parkingBooking) {

        //Διαγραφή μιας κράτησης
        dbTransactions.deleteObject(parkingBooking);
        parkingBookingList.remove(parkingBooking);

        if (FacesContext.getCurrentInstance().getMaximumSeverity() == null) {
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO, "Επιτυχής Διαγραφή κράτησης!", "");
            FacesContext.getCurrentInstance().addMessage(null, msg);
        }
    }


    public void userHasArrived(ParkingBooking parkingBooking) {

        parkingBooking.setHasArrived(true);

        dbTransactions.updateObject(parkingBooking);


        if (FacesContext.getCurrentInstance().getMaximumSeverity() == null) {
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO, "Επιτυχής Άφιξη Χρήστη!", "");
            FacesContext.getCurrentInstance().addMessage(null, msg);
        }
    }

    public void userHasLeft(ParkingBooking parkingBooking) {

        parkingBooking.setHasLeft(true);

        dbTransactions.updateObject(parkingBooking);

        if (FacesContext.getCurrentInstance().getMaximumSeverity() == null) {
                FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO, "Επιτυχής Αναχώρηση Χρήστη!", "");
            FacesContext.getCurrentInstance().addMessage(null, msg);
        }
    }

    public Tbluser getTbluser() {
        return tbluser;
    }

    public void setTbluser(Tbluser tbluser) {
        this.tbluser = tbluser;
    }

    public List<ParkingBooking> getParkingBookingList() {
        return parkingBookingList;
    }

    public void setParkingBookingList(List<ParkingBooking> parkingBookingList) {
        this.parkingBookingList = parkingBookingList;
    }

    public LoginBean getLoginBean() {
        return loginBean;
    }

    public void setLoginBean(LoginBean loginBean) {
        this.loginBean = loginBean;
    }
}

