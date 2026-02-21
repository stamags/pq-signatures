package model;

import jakarta.persistence.*;
import jakarta.xml.bind.annotation.XmlRootElement;
import utils.FileStorageService;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;

/**
 * Created by stamags1988@gmail.com on 14/12/2025.
 */

@Entity
@Table(name = "document_file")
@XmlRootElement
@NamedQueries({
        @NamedQuery(name = "DocumentFile.findAll", query = "SELECT d FROM DocumentFile d ORDER BY d.uploadTime DESC"),
        @NamedQuery(name = "DocumentFile.findById", query = "SELECT d FROM DocumentFile d WHERE d.documentId = :id")
})
public class DocumentFile implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "DOCUMENT_ID", updatable = false, nullable = false)
    private Long documentId;

    @Column(name = "FILENAME", nullable = false, length = 255)
    private String filename;

    @Column(name = "CONTENT_TYPE", length = 100)
    private String contentType;

    @Column(name = "FILE_SIZE")
    private Long fileSize;

    @Column(name = "SHA256", length = 64)
    private String sha256;

    @Column(name = "STORAGE_PATH", length = 500)
    private String storagePath;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "UPLOAD_TIME", nullable = false)
    private Date uploadTime;


    @Column(name = "EMAIL_SENT", nullable = false)
    private Boolean emailSent;

    public DocumentFile() {
    }

    @PrePersist
    protected void onCreate() {
        if (uploadTime == null) uploadTime = new Date();
    }

    // --- Getters/Setters ---

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getSha256() { return sha256; }
    public void setSha256(String sha256) { this.sha256 = sha256; }

    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }

    public Boolean getEmailSent() {return emailSent;}
    public void setEmailSent(Boolean emailSent) {this.emailSent = emailSent;}

    /**
     * Helper method to read file bytes from filesystem.
     * @return file bytes
     * @throws IOException if file not found or read error
     */
    public byte[] getPdfBytes() throws IOException {
        return FileStorageService.readFile(this.documentId);
    }

    public Date getUploadTime() { return uploadTime; }
    public void setUploadTime(Date uploadTime) { this.uploadTime = uploadTime; }

    @Override
    public int hashCode() { return (documentId != null ? documentId.hashCode() : 0); }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DocumentFile)) return false;
        DocumentFile other = (DocumentFile) o;
        if ((this.documentId == null && other.documentId != null) ||
                (this.documentId != null && !this.documentId.equals(other.documentId))) return false;
        return true;
    }

    @Override
    public String toString() {
        return "model.DocumentFile[ documentId=" + documentId + " ]";
    }
}
