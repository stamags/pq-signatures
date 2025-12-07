/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Date;

/**
 * Created by stamags1988@gmail.com on 21/01/2025.
 */
@Entity
@Table(name = "parking_user")
@XmlRootElement
@NamedQueries({
        @NamedQuery(name = "ParkingUser.findAll", query = "SELECT p FROM ParkingUser p")
        , @NamedQuery(name = "ParkingUser.findByUserId", query = "SELECT p FROM ParkingUser p WHERE p.userId = :userId")
        , @NamedQuery(name = "ParkingUser.findByUserName", query = "SELECT p FROM ParkingUser p WHERE p.userName = :userName")
        , @NamedQuery(name = "ParkingUser.findByEmail", query = "SELECT p FROM ParkingUser p WHERE p.email = :email")
        , @NamedQuery(name = "ParkingUser.findByPassword", query = "SELECT p FROM ParkingUser p WHERE p.password = :password")
        , @NamedQuery(name = "ParkingUser.findByIsAuthenticated", query = "SELECT p FROM ParkingUser p WHERE p.isAuthenticated = :isAuthenticated")
        , @NamedQuery(name = "ParkingUser.findBySurname", query = "SELECT p FROM ParkingUser p WHERE p.surname = :surname")
        , @NamedQuery(name = "ParkingUser.findByName", query = "SELECT p FROM ParkingUser p WHERE p.name = :name")
        , @NamedQuery(name = "ParkingUser.findByArithmosKikloforias", query = "SELECT p FROM ParkingUser p WHERE p.airthmosKikloforias = :airthmosKikloforias")
        , @NamedQuery(name = "ParkingUser.findByMarkaAutokinitou", query = "SELECT p FROM ParkingUser p WHERE p.markaAutokinitou = :markaAutokinitou")
        , @NamedQuery(name = "ParkingUser.findByRole", query = "SELECT p FROM ParkingUser p WHERE p.role = :role")})

public class ParkingUser implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @Column(name = "USER_ID")
    private Integer userId;
    @Column(name = "USERNAME")
    private String userName;
    @Column(name = "EMAIL")
    private String email;
    @Column(name = "PASSWORD")
    private String password;
    @Column(name = "ROLE")
    private String role;
    @Column(name = "IS_AUTHENTICATED")
    private Boolean isAuthenticated;
    @Column(name = "SURNAME")
    private String surname;
    @Column(name = "ARITHMOS_KIKLOFORIAS")
    private String airthmosKikloforias;
    @Column(name = "MARKA_AUTOKINHTOU")
    private String markaAutokinitou;
    @Column(name = "NAME")
    private String name;


    public ParkingUser() {
    }

    public ParkingUser(Integer userId, String userName, String email, String password, String role, Boolean isAuthenticated, String surname, String airthmosKikloforias, String markaAutokinitou, String name) {
        this.userId = userId;
        this.userName = userName;
        this.email = email;
        this.password = password;
        this.role = role;
        this.isAuthenticated = isAuthenticated;
        this.surname = surname;
        this.airthmosKikloforias = airthmosKikloforias;
        this.markaAutokinitou = markaAutokinitou;
        this.name = name;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Boolean getIsAuthenticated() {
        return isAuthenticated;
    }

    public void setIsAuthenticated(Boolean isAuthenticated) {
        this.isAuthenticated = isAuthenticated;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getAirthmosKikloforias() {
        return airthmosKikloforias;
    }

    public void setAirthmosKikloforias(String airthmosKikloforias) {
        this.airthmosKikloforias = airthmosKikloforias;
    }

    public String getMarkaAutokinitou() {
        return markaAutokinitou;
    }

    public void setMarkaAutokinitou(String markaAutokinitou) {
        this.markaAutokinitou = markaAutokinitou;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (userId != null ? userId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof ParkingUser)) {
            return false;
        }
        ParkingUser other = (ParkingUser) object;
        if ((this.userId == null && other.userId != null) || (this.userId != null && !this.userId.equals(other.userId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "model.ParkingUser[ userId=" + userId + " ]";
    }

}
