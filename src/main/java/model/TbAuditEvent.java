package model;

import jakarta.persistence.*;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.io.Serializable;
import java.util.Date;

/**
 * Audit event για κάθε κίνηση εγγράφου (upload, sign, verify, email κ.λπ.).
 * Αντιστοιχεί στον πίνακα TB_AUDIT_EVENT.
 */
@Entity
@XmlRootElement
@Table(name = "tb_audit_event", indexes = {
        @Index(name = "IX_AUDIT_DOC_TIME", columnList = "DOCUMENT_ID, CREATED_AT"),
        @Index(name = "IX_AUDIT_USER_TIME", columnList = "USERNAME, CREATED_AT")
})
@NamedQueries({
    @NamedQuery(name = "TbAuditEvent.findAll", query = "SELECT a FROM TbAuditEvent a ORDER BY a.createdAt DESC"),
    @NamedQuery(name = "TbAuditEvent.findByDocumentId", query = "SELECT a FROM TbAuditEvent a WHERE a.documentId = :documentId ORDER BY a.createdAt DESC"),
    @NamedQuery(name = "TbAuditEvent.findByUsername", query = "SELECT a FROM TbAuditEvent a WHERE a.username = :username ORDER BY a.createdAt DESC")
})
public class TbAuditEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "AUDIT_ID", updatable = false, nullable = false)
    private Long auditId;

    @Basic(optional = false)
    @Column(name = "DOCUMENT_ID", nullable = false)
    private Long documentId;

    @Basic(optional = false)
    @Column(name = "USERNAME", nullable = false, length = 100)
    private String username;

    @Basic(optional = false)
    @Column(name = "ACTION_TYPE", nullable = false, length = 30)
    private String actionType;

    @Basic(optional = false)
    @Column(name = "RESULT_STATUS", nullable = false, length = 20)
    private String resultStatus;

    @Column(name = "IP_ADDRESS", length = 64)
    private String ipAddress;

    @Column(name = "USER_AGENT", length = 255)
    private String userAgent;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "CREATED_AT", nullable = false)
    private Date createdAt;

    public TbAuditEvent() {
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = new Date();
        }
    }

    // --- Getters/Setters ---

    public Long getAuditId() {
        return auditId;
    }

    public void setAuditId(Long auditId) {
        this.auditId = auditId;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getResultStatus() {
        return resultStatus;
    }

    public void setResultStatus(String resultStatus) {
        this.resultStatus = resultStatus;
    }


    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
