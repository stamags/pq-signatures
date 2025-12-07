/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

/**
 *
 * @author tsotzo
 */
@Entity
@Table(name = "tbluser")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Tbluser.findAll", query = "SELECT t FROM Tbluser t")
    , @NamedQuery(name = "Tbluser.findByIdUser", query = "SELECT t FROM Tbluser t WHERE t.idUser = :idUser")
    , @NamedQuery(name = "Tbluser.findByUsername", query = "SELECT t FROM Tbluser t WHERE t.username = :username")
    , @NamedQuery(name = "Tbluser.findByPassword", query = "SELECT t FROM Tbluser t WHERE t.password = :password")
    , @NamedQuery(name = "Tbluser.findByCreateTime", query = "SELECT t FROM Tbluser t WHERE t.createTime = :createTime")
    , @NamedQuery(name = "Tbluser.findByUserActive", query = "SELECT t FROM Tbluser t WHERE t.userActive = :userActive")
    , @NamedQuery(name = "Tbluser.findByTheme", query = "SELECT t FROM Tbluser t WHERE t.theme = :theme")
    , @NamedQuery(name = "Tbluser.findByLayoutMode", query = "SELECT t FROM Tbluser t WHERE t.layoutMode = :layoutMode")
    ,@NamedQuery(name = "Tbluser.findByName", query = "SELECT t FROM Tbluser t WHERE t.name = :name")
    , @NamedQuery(name = "Tbluser.findBySurname", query = "SELECT t FROM Tbluser t WHERE t.surname = :surname")
    , @NamedQuery(name = "Tbluser.findByEmail", query = "SELECT t FROM Tbluser t WHERE t.email = :email")
    , @NamedQuery(name = "Tbluser.findByPhone", query = "SELECT t FROM Tbluser t WHERE t.phone = :phone")
        , @NamedQuery(name = "Tbluser.findByResetPassword", query = "SELECT t FROM Tbluser t WHERE t.resetPassword = :resetPassword")
    , @NamedQuery(name = "Tbluser.findByLightMenu", query = "SELECT t FROM Tbluser t WHERE t.lightMenu = :lightMenu")})
public class Tbluser implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "idUser")
    private Integer idUser;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 45)
    @Column(name = "username")
    private String username;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 45)
    @Column(name = "password")
    private String password;
    @Column(name = "create_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createTime;
    @Basic(optional = false)
    @NotNull
    @Column(name = "userActive")
    private int userActive;
    @Size(max = 45)
    @Column(name = "theme")
    private String theme;
    @Size(max = 45)
    @Column(name = "name")
    private String name;
    @Size(max = 45)
    @Column(name = "surname")
    private String surname;
    @Size(max = 45)
    @Column(name = "phone")
    private String phone;
    @Size(max = 45)
    @Column(name = "email")
    private String email;
    @Size(max = 45)
    @Column(name = "layoutMode")
    private String layoutMode;
    @Column(name = "lightMenu")
    private Boolean lightMenu;
    @Column(name = "resetPassword")
    private Boolean resetPassword;
    @Lob
    @Size(max = 65535)
    @Column(name = "avatar")
    private String avatar;
    @JoinColumn(name = "idRole", referencedColumnName = "idRole")
    @ManyToOne(optional = false,fetch = FetchType.LAZY)
    private Tblroles idRole;

    public Tbluser() {
    }

    public Tbluser(Integer idUser) {
        this.idUser = idUser;
    }

    public Tbluser(Integer idUser, String username, String password, int userActive) {
        this.idUser = idUser;
        this.username = username;
        this.password = password;
        this.userActive = userActive;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getIdUser() {
        return idUser;
    }

    public void setIdUser(Integer idUser) {
        this.idUser = idUser;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public int getUserActive() {return userActive;}

    public Boolean getResetPassword() {return resetPassword;}

    public void setResetPassword(Boolean resetPassword) {this.resetPassword = resetPassword;}

    public void setUserActive(int userActive) {
        this.userActive = userActive;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public String getLayoutMode() {
        return layoutMode;
    }

    public void setLayoutMode(String layoutMode) {
        this.layoutMode = layoutMode;
    }

    public Boolean getLightMenu() {
        return lightMenu;
    }

    public void setLightMenu(Boolean lightMenu) {
        this.lightMenu = lightMenu;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public Tblroles getIdRole() {
        return idRole;
    }

    public void setIdRole(Tblroles idRole) {
        this.idRole = idRole;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (idUser != null ? idUser.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Tbluser)) {
            return false;
        }
        Tbluser other = (Tbluser) object;
        if ((this.idUser == null && other.idUser != null) || (this.idUser != null && !this.idUser.equals(other.idUser))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "model.Tbluser[ idUser=" + idUser + " ]";
    }
    
}
