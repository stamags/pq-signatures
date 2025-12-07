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
@Table(name = "parking_floor")
@XmlRootElement
@NamedQueries({
        @NamedQuery(name = "ParkingFloor.findAll", query = "SELECT p FROM ParkingFloor p")
        , @NamedQuery(name = "ParkingFloor.findByFloorId", query = "SELECT p FROM ParkingFloor p WHERE p.floorId = :floorId")
        , @NamedQuery(name = "ParkingFloor.findByFloorName", query = "SELECT p FROM ParkingFloor p WHERE p.floorName = :floorName")})

public class ParkingFloor implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @Column(name = "FLOOR_ID")
    private Integer floorId;
    @Column(name = "FLOOR_NAME")
    private String floorName;



    public ParkingFloor() {
    }

    public ParkingFloor(Integer floorId, String floorName) {
        this.floorId = floorId;
        this.floorName = floorName;
    }

    public Integer getFloorId() {
        return floorId;
    }

    public void setFloorId(Integer floorId) {
        this.floorId = floorId;
    }

    public String getFloorName() {
        return floorName;
    }

    public void setFloorName(String floorName) {
        this.floorName = floorName;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (floorId != null ? floorId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof ParkingFloor)) {
            return false;
        }
        ParkingFloor other = (ParkingFloor) object;
        if ((this.floorId == null && other.floorId != null) || (this.floorId != null && !this.floorId.equals(other.floorId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "model.ParkingFloor[ floorId=" + floorId + " ]";
    }

}
