package model;

import jakarta.persistence.*;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by stamags1988@gmail.com on 14/12/2025.
 */

@Entity
@Table(name = "document_signature")
@XmlRootElement
@NamedQueries({
        @NamedQuery(name = "DocumentSignature.findByDocumentId",
                query = "SELECT s FROM DocumentSignature s WHERE s.documentId.documentId = :docId ORDER BY s.signTime DESC")
})
public class DocumentSignature implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "SIGNATURE_ID", updatable = false, nullable = false)
    private Long signatureId;

    @JoinColumn(name = "DOCUMENT_ID", referencedColumnName = "DOCUMENT_ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private DocumentFile documentId;

    @Column(name = "SCHEME", nullable = false, length = 30)
    private String scheme; // RSA | DILITHIUM | HYBRID

    @Column(name = "HASH_ALG", length = 30)
    private String hashAlg;

    @Lob
    @Column(name = "RSA_SIGNATURE")
    private byte[] rsaSignature;

    @Lob
    @Column(name = "PQC_SIGNATURE")
    private byte[] pqcSignature;

    @Column(name = "RSA_OK")
    private Boolean rsaOk;

    @Column(name = "PQC_OK")
    private Boolean pqcOk;

    @Column(name = "OVERALL_OK")
    private Boolean overallOk;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "SIGN_TIME", nullable = false)
    private Date signTime;

    public DocumentSignature() {}

    @PrePersist
    protected void onCreate() {
        if (signTime == null) signTime = new Date();
        if (hashAlg == null) hashAlg = "SHA-256";
    }

    // --- Getters/Setters ---

    public Long getSignatureId() { return signatureId; }
    public void setSignatureId(Long signatureId) { this.signatureId = signatureId; }

    public DocumentFile getDocumentId() { return documentId; }
    public void setDocumentId(DocumentFile documentId) { this.documentId = documentId; }

    public String getScheme() { return scheme; }
    public void setScheme(String scheme) { this.scheme = scheme; }

    public String getHashAlg() { return hashAlg; }
    public void setHashAlg(String hashAlg) { this.hashAlg = hashAlg; }

    public byte[] getRsaSignature() { return rsaSignature; }
    public void setRsaSignature(byte[] rsaSignature) { this.rsaSignature = rsaSignature; }

    public byte[] getPqcSignature() { return pqcSignature; }
    public void setPqcSignature(byte[] pqcSignature) { this.pqcSignature = pqcSignature; }

    public Boolean getRsaOk() { return rsaOk; }
    public void setRsaOk(Boolean rsaOk) { this.rsaOk = rsaOk; }

    public Boolean getPqcOk() { return pqcOk; }
    public void setPqcOk(Boolean pqcOk) { this.pqcOk = pqcOk; }

    public Boolean getOverallOk() { return overallOk; }
    public void setOverallOk(Boolean overallOk) { this.overallOk = overallOk; }

    public Date getSignTime() { return signTime; }
    public void setSignTime(Date signTime) { this.signTime = signTime; }

    @Override
    public int hashCode() { return (signatureId != null ? signatureId.hashCode() : 0); }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DocumentSignature)) return false;
        DocumentSignature other = (DocumentSignature) o;
        if ((this.signatureId == null && other.signatureId != null) ||
                (this.signatureId != null && !this.signatureId.equals(other.signatureId))) return false;
        return true;
    }

    @Override
    public String toString() {
        return "model.DocumentSignature[ signatureId=" + signatureId + " ]";
    }
}
