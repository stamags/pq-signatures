package utils;

import beans.LoginBean;
import db.dbTransactions;
import jakarta.faces.context.FacesContext;
import model.TbAuditEvent;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Service για καταγραφή audit events κάθε κίνησης εγγράφου (upload, sign, verify, email).
 */
public final class DocumentAuditService {

    private DocumentAuditService() {
    }

    /** Τύποι κίνησης */
    public static final String ACTION_UPLOAD = "UPLOAD";
    public static final String ACTION_SIGN = "SIGN";
    public static final String ACTION_VERIFY = "VERIFY";
    public static final String ACTION_EMAIL_SENT = "EMAIL_SENT";
    public static final String ACTION_DOWNLOAD = "DOWNLOAD";

    /** Κατάσταση αποτελέσματος */
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILURE = "FAILURE";

    /**
     * Καταγράφει ένα audit event με όλες τις παραμέτρους.
     */
    public static void recordEvent(Long documentId, String username, String actionType,
                                   String resultStatus, String ipAddress, String userAgent) {
        if (documentId == null || username == null || actionType == null || resultStatus == null) {
            return;
        }
        TbAuditEvent event = new TbAuditEvent();
        event.setDocumentId(documentId);
        event.setUsername(truncate(username, 100));
        event.setActionType(truncate(actionType, 30));
        event.setResultStatus(truncate(resultStatus, 20));
        event.setIpAddress(ipAddress != null ? truncate(ipAddress, 64) : null);
        event.setUserAgent(userAgent != null ? truncate(userAgent, 255) : null);
        try {
            dbTransactions.storeObject(event);
        } catch (Exception e) {
            // Μην σπάσει η κύρια ροή αν αποτύχει το audit
            org.apache.log4j.Logger.getLogger(DocumentAuditService.class).warn("Audit record failed: " + e.getMessage());
        }
    }

    /**
     * Καταγράφει audit event αντλώντας username από session (LoginBean.AUTH_KEY)
     * και IP / User-Agent από το τρέχον HTTP request, αν υπάρχει FacesContext.
     * Αλλιώς χρησιμοποιεί username "system".
     */
    public static void recordEvent(Long documentId, String actionType, String resultStatus) {
        String username = "system";
        String ipAddress = null;
        String userAgent = null;
        FacesContext fc = FacesContext.getCurrentInstance();
        if (fc != null && fc.getExternalContext() != null) {
            Object auth = fc.getExternalContext().getSessionMap().get(LoginBean.AUTH_KEY);
            if (auth != null && auth.toString().length() > 0) {
                username = auth.toString();
            }
            Object req = fc.getExternalContext().getRequest();
            if (req instanceof HttpServletRequest) {
                HttpServletRequest request = (HttpServletRequest) req;
                ipAddress = request.getRemoteAddr();
                userAgent = request.getHeader("User-Agent");
            }
        }
        recordEvent(documentId, username, actionType, resultStatus, ipAddress, userAgent);
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }
}
