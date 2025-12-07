/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

import java.io.Serializable;
import java.util.List;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

/**
 *
 * @author tsotzo
 */
@Entity
@Table(name = "tblelements")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Tblelements.findAll", query = "SELECT t FROM Tblelements t")
    , @NamedQuery(name = "Tblelements.findByIdelement", query = "SELECT t FROM Tblelements t WHERE t.idelement = :idelement")
    , @NamedQuery(name = "Tblelements.findByElementName", query = "SELECT t FROM Tblelements t WHERE t.elementName = :elementName")
    , @NamedQuery(name = "Tblelements.findByElementDesc", query = "SELECT t FROM Tblelements t WHERE t.elementDesc = :elementDesc")
    , @NamedQuery(name = "Tblelements.findByElementIdDesc", query = "SELECT t FROM Tblelements t WHERE t.elementIdDesc = :elementIdDesc")
    , @NamedQuery(name = "Tblelements.findByVisible", query = "SELECT t FROM Tblelements t WHERE t.visible = :visible")})
public class Tblelements implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "idelement")
    private Integer idelement;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "element_name")
    private String elementName;
    @Size(max = 100)
    @Column(name = "element_desc")
    private String elementDesc;
    @Size(max = 100)
    @Column(name = "element_id_desc")
    private String elementIdDesc;
    @Basic(optional = false)
    @NotNull
    @Column(name = "visible")
    private int visible;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "tblelements")
    private List<Tblrolerights> tblrolerightsList;
    @JoinColumn(name = "ref_page_id", referencedColumnName = "idpage")
    @ManyToOne(fetch = FetchType.LAZY)
    private Tblpages refPageId;

    public Tblelements() {
    }

    public Tblelements(Integer idelement) {
        this.idelement = idelement;
    }

    public Tblelements(Integer idelement, String elementName, int visible) {
        this.idelement = idelement;
        this.elementName = elementName;
        this.visible = visible;
    }

    public Integer getIdelement() {
        return idelement;
    }

    public void setIdelement(Integer idelement) {
        this.idelement = idelement;
    }

    public String getElementName() {
        return elementName;
    }

    public void setElementName(String elementName) {
        this.elementName = elementName;
    }

    public String getElementDesc() {
        return elementDesc;
    }

    public void setElementDesc(String elementDesc) {
        this.elementDesc = elementDesc;
    }

    public String getElementIdDesc() {
        return elementIdDesc;
    }

    public void setElementIdDesc(String elementIdDesc) {
        this.elementIdDesc = elementIdDesc;
    }

    public int getVisible() {
        return visible;
    }

    public void setVisible(int visible) {
        this.visible = visible;
    }

    @XmlTransient
    public List<Tblrolerights> getTblrolerightsList() {
        return tblrolerightsList;
    }

    public void setTblrolerightsList(List<Tblrolerights> tblrolerightsList) {
        this.tblrolerightsList = tblrolerightsList;
    }

    public Tblpages getRefPageId() {
        return refPageId;
    }

    public void setRefPageId(Tblpages refPageId) {
        this.refPageId = refPageId;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (idelement != null ? idelement.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Tblelements)) {
            return false;
        }
        Tblelements other = (Tblelements) object;
        if ((this.idelement == null && other.idelement != null) || (this.idelement != null && !this.idelement.equals(other.idelement))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "model.Tblelements[ idelement=" + idelement + " ]";
    }
    
}
