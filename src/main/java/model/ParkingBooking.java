/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

import org.hibernate.annotations.GenericGenerators;

import jakarta.persistence.*;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.time.Instant;
import java.util.Date;

/**
 * Created by Spiroskleft@gmail.com on 31/10/2017.
 */
@Entity
@Table(name = "parking_booking")
@XmlRootElement
@NamedQueries({
        @NamedQuery(name = "ParkingBooking.findAll", query = "SELECT k FROM ParkingBooking k")
        , @NamedQuery(name = "ParkingBooking.findByBookingId", query = "SELECT k FROM ParkingBooking k WHERE k.bookingId = :bookingId")
        , @NamedQuery(name = "ParkingBooking.findByEndTime", query = "SELECT k FROM ParkingBooking k WHERE k.endTime = :endTime")
        , @NamedQuery(name = "ParkingBooking.findByStartTime", query = "SELECT k FROM ParkingBooking k WHERE k.startTime = :startTime")
        , @NamedQuery(name = "ParkingBooking.findByArithmosKikloforias", query = "SELECT k FROM ParkingBooking k WHERE k.arithmosKikloforias = :arithmosKikloforias")
        , @NamedQuery(name = "ParkingBooking.findByMarkaAutokinitou", query = "SELECT k FROM ParkingBooking k WHERE k.markaAutokinitou = :markaAutokinitou")
        , @NamedQuery(name = "ParkingBooking.findByHasLeft", query = "SELECT k FROM ParkingBooking k WHERE k.hasLeft = :hasLeft")
        , @NamedQuery(name = "ParkingBooking.findBySinolikoKostos", query = "SELECT k FROM ParkingBooking k WHERE k.sinolikoKostos = :sinolikoKostos")
        , @NamedQuery(name = "ParkingBooking.findByHasArrived", query = "SELECT k FROM ParkingBooking k WHERE k.hasArrived = :hasArrived")})


public class ParkingBooking implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "BOOKING_ID", updatable = false, nullable = false)
    private Integer bookingId;
    @Column(name = "START_TIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date startTime;
    @Column(name = "END_TIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date endTime;
    @Column(name = "HAS_ARRIVED")
    private Boolean hasArrived;
    @Column(name = "HAS_LEFT")
    private Boolean hasLeft;
    @Column(name = "ARITHMOS_KIKLOFORIAS")
    private String arithmosKikloforias;
    @Column(name = "MARKA_AUTOKINITOU")
    private String markaAutokinitou;
    @Column(name = "SINOLIKO_KOSTOS")
    private Integer sinolikoKostos;
    @JoinColumn(name = "USER_ID", referencedColumnName = "idUser")
    @ManyToOne(fetch = FetchType.LAZY)
    private Tbluser userId;
    @JoinColumn(name = "SLOT_ID", referencedColumnName = "SLOT_ID")
    @ManyToOne(fetch = FetchType.LAZY)
    private ParkingSlots slotId;


    public ParkingBooking() {
    }

    public ParkingBooking(Integer bookingId, Date startTime, Date endTime, Boolean hasArrived, Boolean hasLeft, String arithmosKikloforias, String markaAutokinitou, Integer sinolikoKostos, Tbluser userId, ParkingSlots slotId) {
        this.bookingId = bookingId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.hasArrived = hasArrived;
        this.hasLeft = hasLeft;
        this.arithmosKikloforias = arithmosKikloforias;
        this.markaAutokinitou = markaAutokinitou;
        this.sinolikoKostos = sinolikoKostos;
        this.userId = userId;
        this.slotId = slotId;
    }

    public Integer getSinolikoKostos() {return sinolikoKostos;}

    public void setSinolikoKostos(Integer sinolikoKostos) {this.sinolikoKostos = sinolikoKostos;}

    public String getArithmosKikloforias() {return arithmosKikloforias;}

    public Boolean getHasLeft() {return hasLeft;}

    public void setHasLeft(Boolean hasLeft) {this.hasLeft = hasLeft;}

    public void setArithmosKikloforias(String arithmosKikloforias) {this.arithmosKikloforias = arithmosKikloforias;}

    public String getMarkaAutokinitou() {return markaAutokinitou;}

    public void setMarkaAutokinitou(String markaAutokinitou) {this.markaAutokinitou = markaAutokinitou;}

    public Integer getBookingId() {
        return bookingId;
    }

    public void setBookingId(Integer bookingId) {
        this.bookingId = bookingId;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public Boolean getHasArrived() {
        return hasArrived;
    }

    public void setHasArrived(Boolean hasArrived) {
        this.hasArrived = hasArrived;
    }

    public Tbluser getUserId() {
        return userId;
    }

    public void setUserId(Tbluser userId) {
        this.userId = userId;
    }

    public ParkingSlots getSlotId() {
        return slotId;
    }

    public void setSlotId(ParkingSlots slotId) {
        this.slotId = slotId;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (bookingId != null ? bookingId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof ParkingBooking)) {
            return false;
        }
        ParkingBooking other = (ParkingBooking) object;
        if ((this.bookingId == null && other.bookingId != null) || (this.bookingId != null && !this.bookingId.equals(other.bookingId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "model.ParkinhgBooking[ bookingId=" + bookingId + " ]";
    }


}
