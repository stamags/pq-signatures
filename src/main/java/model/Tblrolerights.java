/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

import java.io.Serializable;
import jakarta.persistence.*;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author tsotzo
 */
@Entity
@Table(name = "tblrolerights")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Tblrolerights.findAll", query = "SELECT t FROM Tblrolerights t")
    , @NamedQuery(name = "Tblrolerights.findByIdRole", query = "SELECT t FROM Tblrolerights t WHERE t.tblrolerightsPK.idRole = :idRole")
    , @NamedQuery(name = "Tblrolerights.findByIdPage", query = "SELECT t FROM Tblrolerights t WHERE t.tblrolerightsPK.idPage = :idPage")
    , @NamedQuery(name = "Tblrolerights.findByIdElement", query = "SELECT t FROM Tblrolerights t WHERE t.tblrolerightsPK.idElement = :idElement")})
public class Tblrolerights implements Serializable {

    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected TblrolerightsPK tblrolerightsPK;
    @JoinColumn(name = "idElement", referencedColumnName = "idelement", insertable = false, updatable = false)
    @ManyToOne(optional = false,fetch = FetchType.LAZY)
    private Tblelements tblelements;
    @JoinColumn(name = "idPage", referencedColumnName = "idpage", insertable = false, updatable = false)
    @ManyToOne(optional = false,fetch = FetchType.LAZY)
    private Tblpages tblpages;
    @JoinColumn(name = "idRole", referencedColumnName = "idRole", insertable = false, updatable = false)
    @ManyToOne(optional = false,fetch = FetchType.LAZY)
    private Tblroles tblroles;

    public Tblrolerights() {
    }

    public Tblrolerights(TblrolerightsPK tblrolerightsPK) {
        this.tblrolerightsPK = tblrolerightsPK;
    }

    public Tblrolerights(int idRole, int idPage, int idElement) {
        this.tblrolerightsPK = new TblrolerightsPK(idRole, idPage, idElement);
    }

    public TblrolerightsPK getTblrolerightsPK() {
        return tblrolerightsPK;
    }

    public void setTblrolerightsPK(TblrolerightsPK tblrolerightsPK) {
        this.tblrolerightsPK = tblrolerightsPK;
    }

    public Tblelements getTblelements() {
        return tblelements;
    }

    public void setTblelements(Tblelements tblelements) {
        this.tblelements = tblelements;
    }

    public Tblpages getTblpages() {
        return tblpages;
    }

    public void setTblpages(Tblpages tblpages) {
        this.tblpages = tblpages;
    }

    public Tblroles getTblroles() {
        return tblroles;
    }

    public void setTblroles(Tblroles tblroles) {
        this.tblroles = tblroles;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (tblrolerightsPK != null ? tblrolerightsPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Tblrolerights)) {
            return false;
        }
        Tblrolerights other = (Tblrolerights) object;
        if ((this.tblrolerightsPK == null && other.tblrolerightsPK != null) || (this.tblrolerightsPK != null && !this.tblrolerightsPK.equals(other.tblrolerightsPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "model.Tblrolerights[ tblrolerightsPK=" + tblrolerightsPK + " ]";
    }
    
}
