/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package beans;


import db.HashingUtil;
import db.dbTransactions;
import model.ParkingUsersCar;
import model.Tbluser;


import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;

import jakarta.faces.application.FacesMessage;

import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;




/**
 * @author stamags1988@gmail.com
 */

@Named("UserpreferencesBean")
@SessionScoped
public class UserpreferencesBean implements Serializable {

    private Tbluser tbluser = new Tbluser();
    private ParkingUsersCar parkingUsersCar = new ParkingUsersCar();
    private List<ParkingUsersCar> parkingUsersCarList = new ArrayList<>();
    private Boolean newparkingUsersCar = false;


    private String filename;


    //Login Bean
    @Inject
    private LoginBean loginBean;

    @PostConstruct
    public void init() {
        tbluser = loginBean.getUser();

        String parkingUserCarQuery = "from parking_users_car e where e.idUser=" + tbluser.getIdUser() + ";";
        parkingUsersCarList = (List<ParkingUsersCar>) (List<?>) dbTransactions.getObjectsBySqlQuery(ParkingUsersCar.class, parkingUserCarQuery, null, null, null);


        if (Objects.isNull(parkingUsersCarList) || parkingUsersCarList.size() == 0) {
            newparkingUsersCar= true;
            parkingUsersCar = new ParkingUsersCar();
        } else {
            newparkingUsersCar= false;
            parkingUsersCar = parkingUsersCarList.get(0);
        }
    }


    public void storeObjects() {


        tbluser.setPassword(HashingUtil.SHA1(tbluser.getPassword()));

        try {
            dbTransactions.updateObject(tbluser);
            if (newparkingUsersCar){
                dbTransactions.storeObject(parkingUsersCar);
            }else {
                dbTransactions.updateObject(parkingUsersCar);
            }


            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO, "Επιτυχής αποθήκευση των δεδομένων σας!", "");
            FacesContext.getCurrentInstance().addMessage(null, msg);

            
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }




    public LoginBean getLoginBean() {
        return loginBean;
    }

    public void setLoginBean(LoginBean loginBean) {
        this.loginBean = loginBean;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Tbluser getTbluser() {
        return tbluser;
    }

    public void setTbluser(Tbluser tbluser) {
        this.tbluser = tbluser;
    }

    public ParkingUsersCar getParkingUsersCar() {
        return parkingUsersCar;
    }

    public void setParkingUsersCar(ParkingUsersCar parkingUsersCar) {
        this.parkingUsersCar = parkingUsersCar;
    }

}

