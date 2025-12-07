/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package beans;

import model.Tblroles;
import org.omnifaces.util.Ajax;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.List;

/**
 * @author takis
 */

@Named("RolesBean")
@ViewScoped
public class Roles implements Serializable {

    private List<Tblroles> rolesList;
    private String infomsg;
    private Tblroles role = new Tblroles();
    private Tblroles roleCopy = new Tblroles();

    //Login Bean
    @Inject
    private LoginBean loginBean;


    @PostConstruct
    public void init() {
        rolesList = (List<Tblroles>) (List<?>) db.dbTransactions.getAllObjectsSorted(Tblroles.class.getCanonicalName(), "roleDesc", 0);
        clear1();
    }

    /**
     * Αντιγράφει το Object για να μπορέσουμε να κάνουμε τυο Update.
     */
    public void copyObject() {
        roleCopy = role;
    }

    public void insert() {
        FacesMessage message = new FacesMessage();

        if (role != null) {
            db.dbTransactions.storeObject(role);
        } else {
            message = new FacesMessage(FacesMessage.SEVERITY_INFO, "Fault Insert", role.getRoleDesc());
        }
        if (FacesContext.getCurrentInstance().getMaximumSeverity() == null) {

            rolesList.add(role);

            FacesMessage msg = new FacesMessage("Successful Insert", (role.getRoleDesc()));
            FacesContext.getCurrentInstance().addMessage(null, msg);

        }
    }


    public void update() {
        FacesMessage message = new FacesMessage();

        if (role != null) {
            db.dbTransactions.updateObject(role);
        } else {
            message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fault Update", role.getRoleDesc());
        }
        if (FacesContext.getCurrentInstance().getMaximumSeverity() == null) {

            int index = rolesList.indexOf(roleCopy);

            rolesList.set(index, role);

            FacesMessage msg = new FacesMessage("Successful Update", (role.getRoleDesc()));
            FacesContext.getCurrentInstance().addMessage(null, msg);


        }
    }


    public void delete() {

        db.dbTransactions.deleteObject(role);

        if (FacesContext.getCurrentInstance().getMaximumSeverity() == null) {
            FacesMessage msg = new FacesMessage("Successful Delete");
            FacesContext.getCurrentInstance().addMessage(null, msg);

            rolesList.remove(role);
            Ajax.update("form");

        }
    }


    public void addMessage(String summary) {
        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, summary, null);
        FacesContext.getCurrentInstance().addMessage(null, message);
    }


    public void clear1() {

        role = new Tblroles();
    }

    /**
     * @return the infomsg
     */
    public String getInfomsg() {
        return infomsg;
    }

    /**
     * @param infomsg the infomsg to set
     */
    public void setInfomsg(String infomsg) {
        this.infomsg = infomsg;
    }

    /**
     * @return the rolesList
     */
    public List<Tblroles> getRolesList() {
        return rolesList;
    }

    /**
     * @param RolesList the rolesList to set
     */
    public void setRolesList(List<Tblroles> RolesList) {
        this.rolesList = RolesList;
    }

    /**
     * @return the role
     */
    public Tblroles getRole() {
        return role;
    }

    /**
     * @param Role the role to set
     */
    public void setRole(Tblroles Role) {
        this.role = Role;
    }


    public LoginBean getLoginBean() {
        return loginBean;
    }

    public void setLoginBean(LoginBean loginBean) {
        this.loginBean = loginBean;
    }
}

