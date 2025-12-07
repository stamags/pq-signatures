package model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.math.BigInteger;
import java.sql.Blob;
import java.util.Date;

@Entity
@Table(name = "entolh_elegxou")
@XmlRootElement
public class TblCheckCommand {

    private static final long serialVersionUID = 1L;

    @Id
    @Basic(optional = false)
    @NotNull
    @Column(name = "tayt_entolhs_epith")
    @Size(max = 25)
    private String commandInspectionId;

    @Basic(optional = false)
    @NotNull
    @Column(name = "diaxeirhsh_kwd")
    @Size(max = 10)
    private BigInteger administrationId;

    @Size(max = 100)
    @Column(name = "kaep")
    private String kaep;

    @Column(name = "locked")
    private Boolean locked;

    @Basic(optional = false)
    @NotNull
    @Column(name = "hmer_entolhs")
    private Date commandDate;

    @Column(name = "hmer_enarkshs")
    private Date startDate;

    @Column(name = "hmer_lhkshs")
    private Date endDate;

    @Size(max = 100)
    @Column(name = "taytothta_metrwn")
    private String measuresId;

    @Basic(optional = false)
    @NotNull
    @Size(max = 10)
    @Column(name = "entolh_elegxou_kwd")
    private BigInteger commandCheckId;

    @Column(name = "hmer_epitheorhshs")
    private Date inspectionDate;

    @Size(max = 10)
    @Column(name = "epith_kwd")
    private Integer inspectionId;

    @Size(max = 10)
    @Column(name = "apot_epith_kwd")
    private Integer inspectionResultId;

    @Column(name = "pdf_doc")
    private Blob pdfDoc;

    @Column(name = "hmer_metrwn")
    private Date measuresDate;

    @Size(max = 100)
    @Column(name = "tayt_diabibastikou")
    private String transitoryId;

    @Column(name = "hmer_diabibastikou")
    private Date transitoryDate;

    @Size(max = 500)
    @Column(name = "diafores")
    private String differences;

    @Column(name = "hmer_oloklhrwsis")
    private Date completionDate;

    @Size(max = 100)
    @Column(name = "tayt_oloklhrwshs")
    private String completionId;

    @Size(max = 10)
    @Column(name = "kat_epith_kwd")
    private BigInteger catInspectionId;

    @Size(max = 10)
    @Column(name = "eid_prot_kwd")
    private BigInteger eidProtId;

    public TblCheckCommand() {
    }

    public TblCheckCommand(String commandInspectionId, BigInteger administrationId, Date commandDate, BigInteger commandCheckId) {
        this.commandInspectionId = commandInspectionId;
        this.administrationId = administrationId;
        this.commandDate = commandDate;
        this.commandCheckId = commandCheckId;
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public String getCommandInspectionId() {
        return commandInspectionId;
    }

    public void setCommandInspectionId(String commandInspectionId) {
        this.commandInspectionId = commandInspectionId;
    }

    public BigInteger getAdministrationId() {
        return administrationId;
    }

    public void setAdministrationId(BigInteger administrationId) {
        this.administrationId = administrationId;
    }

    public String getKaep() {
        return kaep;
    }

    public void setKaep(String kaep) {
        this.kaep = kaep;
    }

    public Boolean getLocked() {
        return locked;
    }

    public void setLocked(Boolean locked) {
        this.locked = locked;
    }

    public Date getCommandDate() {
        return commandDate;
    }

    public void setCommandDate(Date commandDate) {
        this.commandDate = commandDate;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public String getMeasuresId() {
        return measuresId;
    }

    public void setMeasuresId(String measuresId) {
        this.measuresId = measuresId;
    }

    public BigInteger getCommandCheckId() {
        return commandCheckId;
    }

    public void setCommandCheckId(BigInteger commandCheckId) {
        this.commandCheckId = commandCheckId;
    }

    public Date getInspectionDate() {
        return inspectionDate;
    }

    public void setInspectionDate(Date inspectionDate) {
        this.inspectionDate = inspectionDate;
    }

    public Integer getInspectionId() {
        return inspectionId;
    }

    public void setInspectionId(Integer inspectionId) {
        this.inspectionId = inspectionId;
    }

    public Integer getInspectionResultId() {
        return inspectionResultId;
    }

    public void setInspectionResultId(Integer inspectionResultId) {
        this.inspectionResultId = inspectionResultId;
    }

    public Blob getPdfDoc() {
        return pdfDoc;
    }

    public void setPdfDoc(Blob pdfDoc) {
        this.pdfDoc = pdfDoc;
    }

    public Date getMeasuresDate() {
        return measuresDate;
    }

    public void setMeasuresDate(Date measuresDate) {
        this.measuresDate = measuresDate;
    }

    public String getTransitoryId() {
        return transitoryId;
    }

    public void setTransitoryId(String transitoryId) {
        this.transitoryId = transitoryId;
    }

    public Date getTransitoryDate() {
        return transitoryDate;
    }

    public void setTransitoryDate(Date transitoryDate) {
        this.transitoryDate = transitoryDate;
    }

    public String getDifferences() {
        return differences;
    }

    public void setDifferences(String differences) {
        this.differences = differences;
    }

    public Date getCompletionDate() {
        return completionDate;
    }

    public void setCompletionDate(Date completionDate) {
        this.completionDate = completionDate;
    }

    public String getCompletionId() {
        return completionId;
    }

    public void setCompletionId(String completionId) {
        this.completionId = completionId;
    }

    public BigInteger getCatInspectionId() {
        return catInspectionId;
    }

    public void setCatInspectionId(BigInteger catInspectionId) {
        this.catInspectionId = catInspectionId;
    }

    public BigInteger getEidProtId() {
        return eidProtId;
    }

    public void setEidProtId(BigInteger eidProtId) {
        this.eidProtId = eidProtId;
    }
}
