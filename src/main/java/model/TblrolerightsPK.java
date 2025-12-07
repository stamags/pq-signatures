/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

import java.io.Serializable;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;

/**
 *
 * @author tsotzo
 */
@Embeddable
public class TblrolerightsPK implements Serializable {

    @Basic(optional = false)
    @NotNull
    @Column(name = "idRole")
    private int idRole;
    @Basic(optional = false)
    @NotNull
    @Column(name = "idPage")
    private int idPage;
    @Basic(optional = false)
    @NotNull
    @Column(name = "idElement")
    private int idElement;

    public TblrolerightsPK() {
    }

    public TblrolerightsPK(int idRole, int idPage, int idElement) {
        this.idRole = idRole;
        this.idPage = idPage;
        this.idElement = idElement;
    }

    public int getIdRole() {
        return idRole;
    }

    public void setIdRole(int idRole) {
        this.idRole = idRole;
    }

    public int getIdPage() {
        return idPage;
    }

    public void setIdPage(int idPage) {
        this.idPage = idPage;
    }

    public int getIdElement() {
        return idElement;
    }

    public void setIdElement(int idElement) {
        this.idElement = idElement;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (int) idRole;
        hash += (int) idPage;
        hash += (int) idElement;
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof TblrolerightsPK)) {
            return false;
        }
        TblrolerightsPK other = (TblrolerightsPK) object;
        if (this.idRole != other.idRole) {
            return false;
        }
        if (this.idPage != other.idPage) {
            return false;
        }
        if (this.idElement != other.idElement) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "model.TblrolerightsPK[ idRole=" + idRole + ", idPage=" + idPage + ", idElement=" + idElement + " ]";
    }
    
}
