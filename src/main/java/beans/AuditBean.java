package beans;

import db.dbTransactions;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import model.DocumentFile;

import model.TbAuditEvent;
import utils.DocumentAuditService;
import utils.EmailService;
import utils.FacesUtil;
import utils.FileStorageService;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Named("auditBean")
@ViewScoped
public class AuditBean implements Serializable {

    private Long docId;

    private Boolean hasResult = false;
    private Long documentIdToDownload;
    private List<DocumentFile> documentFileList = new ArrayList<>();
    private List<TbAuditEvent> tbAuditEventList = new ArrayList<>();


    public AuditBean() {

    }

    @Inject
    private EmailService emailService;

    @PostConstruct
    public void init() throws IOException {


        documentFileList = (List<DocumentFile>) (List<?>) dbTransactions.getAllObjects(DocumentFile.class.getCanonicalName());


    }

    public void epilogiArxeiou(DocumentFile documentFile) {

        docId = documentFile.getDocumentId();


        if (FacesContext.getCurrentInstance().getMaximumSeverity() == null) {
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO, "Επιλέξατα το αρχείο  με όνομα ", documentFile.getFilename());
            FacesContext.getCurrentInstance().addMessage(null, msg);
        }

    }

    public void audit() {
        try {
            if (docId == null) {
                FacesMessage facesMessage = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Παρακαλώ προσθέστε τον αριθμό id του εγγράφου", "");
                FacesContext.getCurrentInstance().addMessage("fail", facesMessage);
                return;
            }

            String tbAuditEventQuery = "from tb_audit_event e where e.document_Id=" + docId + ";";
            tbAuditEventList = (List<TbAuditEvent>) (List<?>) dbTransactions.getObjectsBySqlQuery(TbAuditEvent.class, tbAuditEventQuery, null, null, null);

            if (!Objects.isNull(tbAuditEventList) && tbAuditEventList.size() > 0) {
                hasResult = true;
                FacesMessage facesMessage = new FacesMessage(FacesMessage.SEVERITY_INFO, "Επιτυχής φόρτωση ιστορικού", "");
                FacesContext.getCurrentInstance().addMessage("success", facesMessage);
            }else {
                FacesMessage facesMessage = new FacesMessage(FacesMessage.SEVERITY_WARN, "Δεν βρέθηκε ιστορικό ενεργειών για το επιλεγμένο έγγραφο", "");
                FacesContext.getCurrentInstance().addMessage("success", facesMessage);
            }

        } catch (
                IllegalArgumentException e) {
            hasResult = false;
        }
    }

    /**
     * Λήψη (download) του υπογεγραμμένου PDF. Καταγράφεται στο audit ως ACTION_DOWNLOAD.
     * Το documentId ορίζεται μέσω setPropertyActionListener πριν την κλήση.
     */
    public void downloadDocument() {
        Long docId = documentIdToDownload;
        documentIdToDownload = null;
        if (docId == null) {
            FacesUtil.error("Δεν επιλέχθηκε έγγραφο.");
            return;
        }
        try {
            DocumentFile doc = db.dbTransactions.getObjectById(DocumentFile.class, docId);
            if (doc == null) {
                DocumentAuditService.recordEvent(docId, DocumentAuditService.ACTION_DOWNLOAD, DocumentAuditService.STATUS_FAILURE);
                FacesUtil.error("Το έγγραφο δεν βρέθηκε.");
                return;
            }
            byte[] bytes = FileStorageService.readFile(docId);
            String filename = doc.getFilename() != null && !doc.getFilename().isBlank()
                    ? doc.getFilename()
                    : "document-" + docId + ".pdf";
            String safeName = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", " ");

            FacesContext fc = FacesContext.getCurrentInstance();
            ExternalContext ec = fc.getExternalContext();
            ec.responseReset();
            ec.setResponseContentType("application/pdf");
            ec.setResponseContentLength(bytes.length);
            ec.setResponseHeader("Content-Disposition", "attachment; filename=\"" + safeName + "\"; filename*=UTF-8''" + safeName);
            try (OutputStream out = ec.getResponseOutputStream()) {
                out.write(bytes);
                out.flush();
            }
            fc.responseComplete();
            DocumentAuditService.recordEvent(docId, DocumentAuditService.ACTION_DOWNLOAD, DocumentAuditService.STATUS_SUCCESS);
        } catch (Exception e) {
            DocumentAuditService.recordEvent(docId, DocumentAuditService.ACTION_DOWNLOAD, DocumentAuditService.STATUS_FAILURE);
            FacesUtil.error("Σφάλμα λήψης: " + e.getMessage());
        }
    }

    public boolean isHasResult() {
        return hasResult;
    }

    public Long getDocId() {
        return docId;
    }

    public void setDocId(Long docId) {
        this.docId = docId;
    }



    public List<DocumentFile> getDocumentFileList() {
        return documentFileList;
    }

    public void setDocumentFileList(List<DocumentFile> documentFileList) {
        this.documentFileList = documentFileList;
    }

    public List<TbAuditEvent> getTbAuditEventList() {
        return tbAuditEventList;
    }

    public void setTbAuditEventList(List<TbAuditEvent> tbAuditEventList) {
        this.tbAuditEventList = tbAuditEventList;
    }

    public void setHasResult(Boolean hasResult) {
        this.hasResult = hasResult;
    }

    public void setDocumentIdToDownload(Long documentIdToDownload) {
        this.documentIdToDownload = documentIdToDownload;
    }

    public void setEmailService(EmailService emailService) {
        this.emailService = emailService;
    }
}

