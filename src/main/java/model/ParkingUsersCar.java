/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

import jakarta.persistence.*;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * Created by stamags1988@gmail.com on 21/01/2025.
 */
@Entity
@Table(name = "parking_users_car")
@XmlRootElement
@NamedQueries({
        @NamedQuery(name = "ParkingUsersCar.findAll", query = "SELECT p FROM ParkingUsersCar p")
        , @NamedQuery(name = "ParkingUsersCar.findById", query = "SELECT p FROM ParkingUsersCar p WHERE p.id = :id")
        , @NamedQuery(name = "ParkingUsersCar.findByArithmosKikloforias", query = "SELECT p FROM ParkingUsersCar p WHERE p.arithmosKikloforias = :arithmosKikloforias")
        , @NamedQuery(name = "ParkingUsersCar.findByMarkaAutokinitou", query = "SELECT p FROM ParkingUsersCar p WHERE p.markaAutokinitou = :markaAutokinitou")})

public class ParkingUsersCar implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "ID")
    private Integer id;
    @Column(name = "ARITHMOS_KIKLOFORIAS")
    private String arithmosKikloforias;
    @Column(name = "MARKA_AUTOKINITOU")
    private String markaAutokinitou;
    @JoinColumn(name = "idUser", referencedColumnName = "idUser")
    @ManyToOne(fetch = FetchType.LAZY)
    private Tbluser idUser;


    public ParkingUsersCar() {
    }

    public ParkingUsersCar(Integer id, String arithmosKikloforias, String markaAutokinitou, Tbluser idUser) {
        this.id = id;
        this.arithmosKikloforias = arithmosKikloforias;
        this.markaAutokinitou = markaAutokinitou;
        this.idUser = idUser;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getArithmosKikloforias() {
        return arithmosKikloforias;
    }

    public void setArithmosKikloforias(String arithmosKikloforias) {
        this.arithmosKikloforias = arithmosKikloforias;
    }

    public String getMarkaAutokinitou() {
        return markaAutokinitou;
    }

    public void setMarkaAutokinitou(String markaAutokinitou) {
        this.markaAutokinitou = markaAutokinitou;
    }

    public Tbluser getIdUser() {
        return idUser;
    }

    public void setIdUser(Tbluser idUser) {
        this.idUser = idUser;
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
        if (!(object instanceof ParkingUsersCar)) {
            return false;
        }
        ParkingUsersCar other = (ParkingUsersCar) object;
        if ((this.idUser == null && other.idUser != null) || (this.idUser != null && !this.idUser.equals(other.idUser))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "model.ParkingUser[ userId=" + idUser + " ]";
    }

}
