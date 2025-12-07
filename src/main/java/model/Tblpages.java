/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

import java.io.Serializable;
import java.util.List;
import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

/**
 *
 * @author tsotzo
 */
@Entity
@Table(name = "tblpages")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Tblpages.findAll", query = "SELECT t FROM Tblpages t")
    , @NamedQuery(name = "Tblpages.findByIdpage", query = "SELECT t FROM Tblpages t WHERE t.idpage = :idpage")
    , @NamedQuery(name = "Tblpages.findByPageName", query = "SELECT t FROM Tblpages t WHERE t.pageName = :pageName")
    , @NamedQuery(name = "Tblpages.findByPageDesc", query = "SELECT t FROM Tblpages t WHERE t.pageDesc = :pageDesc")})
public class Tblpages implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "idpage")
    private Integer idpage;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "page_name")
    private String pageName;
    @Size(max = 100)
    @Column(name = "page_desc")
    private String pageDesc;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "tblpages")
    private List<Tblrolerights> tblrolerightsList;
    @OneToMany(mappedBy = "refPageId")
    private List<Tblelements> tblelementsList;

    public Tblpages() {
    }

    public Tblpages(Integer idpage) {
        this.idpage = idpage;
    }

    public Tblpages(Integer idpage, String pageName) {
        this.idpage = idpage;
        this.pageName = pageName;
    }

    public Integer getIdpage() {
        return idpage;
    }

    public void setIdpage(Integer idpage) {
        this.idpage = idpage;
    }

    public String getPageName() {
        return pageName;
    }

    public void setPageName(String pageName) {
        this.pageName = pageName;
    }

    public String getPageDesc() {
        return pageDesc;
    }

    public void setPageDesc(String pageDesc) {
        this.pageDesc = pageDesc;
    }

    @XmlTransient
    public List<Tblrolerights> getTblrolerightsList() {
        return tblrolerightsList;
    }

    public void setTblrolerightsList(List<Tblrolerights> tblrolerightsList) {
        this.tblrolerightsList = tblrolerightsList;
    }

    @XmlTransient
    public List<Tblelements> getTblelementsList() {
        return tblelementsList;
    }

    public void setTblelementsList(List<Tblelements> tblelementsList) {
        this.tblelementsList = tblelementsList;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (idpage != null ? idpage.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Tblpages)) {
            return false;
        }
        Tblpages other = (Tblpages) object;
        if ((this.idpage == null && other.idpage != null) || (this.idpage != null && !this.idpage.equals(other.idpage))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "model.Tblpages[ idpage=" + idpage + " ]";
    }
    
}
