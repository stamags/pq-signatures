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
@Table(name = "parking_slots")
@XmlRootElement
@NamedQueries({
        @NamedQuery(name = "ParkingSlots.findAll", query = "SELECT p FROM ParkingSlots p")
        , @NamedQuery(name = "ParkingSlots.findBySlotId", query = "SELECT p FROM ParkingSlots p WHERE p.slotId = :slotId")
        , @NamedQuery(name = "ParkingSlots.findByFloorId", query = "SELECT p FROM ParkingSlots p WHERE p.floorId = :floorId")
        , @NamedQuery(name = "ParkingSlots.findBySlotNumber", query = "SELECT p FROM ParkingSlots p WHERE p.slotNumber = :slotNumber")
        , @NamedQuery(name = "ParkingSlots.findByIsAvailable", query = "SELECT p FROM ParkingSlots p WHERE p.isAvailable = :isAvailable")
        , @NamedQuery(name = "ParkingSlots.findByPricePerHour", query = "SELECT p FROM ParkingSlots p WHERE p.pricePerHour = :pricePerHour")})

public class ParkingSlots implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @Column(name = "SLOT_ID")
    private Integer slotId;
    @Column(name = "SLOT_NUMBER")
    private Integer slotNumber;
    @Column(name = "IS_AVAILABLE")
    private String isAvailable;
    @Column(name = "PRICE_PER_HOUR")
    private String pricePerHour;
    @JoinColumn(name = "FLOOR_ID", referencedColumnName = "FLOOR_ID")
    @ManyToOne(fetch = FetchType.LAZY)
    private ParkingFloor floorId;


    public ParkingSlots() {

    }

    public ParkingSlots(Integer slotId, Integer slotNumber, String isAvailable, String pricePerHour, ParkingFloor floorId) {
        this.slotId = slotId;
        this.slotNumber = slotNumber;
        this.isAvailable = isAvailable;
        this.pricePerHour = pricePerHour;
        this.floorId = floorId;
    }

    public Integer getSlotId() {
        return slotId;
    }

    public void setSlotId(Integer slotId) {
        this.slotId = slotId;
    }

    public Integer getSlotNumber() {
        return slotNumber;
    }

    public void setSlotNumber(Integer slotNumber) {
        this.slotNumber = slotNumber;
    }

    public String getIsAvailable() {
        return isAvailable;
    }

    public void setIsAvailable(String isAvailable) {
        this.isAvailable = isAvailable;
    }

    public String getPricePerHour() {
        return pricePerHour;
    }

    public void setPricePerHour(String pricePerHour) {
        this.pricePerHour = pricePerHour;
    }

    public ParkingFloor getFloorId() {
        return floorId;
    }

    public void setFloorId(ParkingFloor floorId) {
        this.floorId = floorId;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (slotId != null ? slotId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof ParkingSlots)) {
            return false;
        }
        ParkingSlots other = (ParkingSlots) object;
        if ((this.slotId == null && other.slotId != null) || (this.slotId != null && !this.slotId.equals(other.slotId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "model.ParkingSlots[ slotId=" + slotId + " ]";
    }

}
