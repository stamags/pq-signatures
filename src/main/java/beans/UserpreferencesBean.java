/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package beans;


import db.HashingUtil;
import model.Tbluser;


import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;


import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.*;






/**
 * @author stamags1988@gmail.com
 */

@Named("UserpreferencesBean")
@SessionScoped
public class UserpreferencesBean implements Serializable {

    private Tbluser tbluser = new Tbluser();
    private Boolean newparkingUsersCar = false;


    private String filename;


    //Login Bean
    @Inject
    private LoginBean loginBean;

    @PostConstruct
    public void init() {
        tbluser = loginBean.getUser();

    }


    public void storeObjects() {
        tbluser.setPassword(HashingUtil.SHA1(tbluser.getPassword()));
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


}

