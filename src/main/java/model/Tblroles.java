/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

import java.io.Serializable;
import java.util.List;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

/**
 *
 * @author tsotzo
 */
@Entity
@Table(name = "tblroles")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Tblroles.findAll", query = "SELECT t FROM Tblroles t")
    , @NamedQuery(name = "Tblroles.findByIdRole", query = "SELECT t FROM Tblroles t WHERE t.idRole = :idRole")
    , @NamedQuery(name = "Tblroles.findByRoleDesc", query = "SELECT t FROM Tblroles t WHERE t.roleDesc = :roleDesc")})
public class Tblroles implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "idRole")
    private Integer idRole;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 45)
    @Column(name = "RoleDesc")
    private String roleDesc;
    

    public Tblroles() {
    }

    public Tblroles(Integer idRole) {
        this.idRole = idRole;
    }

    public Tblroles(Integer idRole, String roleDesc) {
        this.idRole = idRole;
        this.roleDesc = roleDesc;
    }

    public Integer getIdRole() {
        return idRole;
    }

    public void setIdRole(Integer idRole) {
        this.idRole = idRole;
    }

    public String getRoleDesc() {
        return roleDesc;
    }

    public void setRoleDesc(String roleDesc) {
        this.roleDesc = roleDesc;
    }


    @Override
    public int hashCode() {
        int hash = 0;
        hash += (idRole != null ? idRole.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Tblroles)) {
            return false;
        }
        Tblroles other = (Tblroles) object;
        if ((this.idRole == null && other.idRole != null) || (this.idRole != null && !this.idRole.equals(other.idRole))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "model.Tblroles[ idRole=" + idRole + " ]";
    }
    
}
