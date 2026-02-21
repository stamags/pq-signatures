package beans;

import db.JPAUtil;
import db.dbTransactions;
import jakarta.annotation.PostConstruct;
import utils.DocumentAuditService;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Bean για το Dashboard / Στατιστικά: πλήθος εγγράφων, επαληθεύσεις, events ανά ημέρα/χρήστη.
 */
@Named("dashboardBean")
@ViewScoped
public class DashboardBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private int totalDocuments;
    private int verifySuccessCount;
    private int verifyFailureCount;
    private int totalAuditEvents;
    /** Έγγραφα που έχουν σταλεί με email */
    private int emailSentCount;
    /** Έγγραφα που δεν έχουν σταλεί */
    private int emailNotSentCount;
    /** Ανυπόγραφα, μόνο RSA, μόνο Dilithium, υβριδικά */
    private int unsignedCount;
    private int rsaOnlyCount;
    private int dilithiumOnlyCount;
    private int hybridCount;

    /** Events ανά τύπο ενέργειας (UPLOAD, SIGN, VERIFY, EMAIL_SENT, DOWNLOAD) */
    private int uploadCount;
    private int signCount;
    private int verifyCount;
    private int emailSentCountAudit;
    private int downloadCount;

    /** Πλήθος λήψεων (DOWNLOAD events) */
    private int totalDownloads;
    /** Ποσοστό επιτυχίας επαληθεύσεων (0–100), -1 αν δεν υπάρχουν επαληθεύσεις */
    private double verifySuccessRatePercent = -1;

    /** Γραμμές για πίνακα "Events ανά ημέρα": (ημερομηνία, πλήθος) */
    private List<EventCountRow> eventsPerDay = new ArrayList<>();
    /** Γραμμές για πίνακα "Events ανά χρήστη": (username, πλήθος) */
    private List<EventCountRow> eventsPerUser = new ArrayList<>();

    public static class EventCountRow implements Serializable {
        private static final long serialVersionUID = 1L;
        private String label;
        private Long count;

        public EventCountRow(String label, Long count) {
            this.label = label;
            this.count = count;
        }

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public Long getCount() { return count; }
        public void setCount(Long count) { this.count = count; }
    }

    @PostConstruct
    public void init() {
        loadStats();
    }

    public void refresh() {
        loadStats();
    }

    @SuppressWarnings("unchecked")
    private void loadStats() {
        // Πλήθος εγγράφων
        Integer docCount = dbTransactions.countAllObjects(model.DocumentFile.class.getCanonicalName());
        totalDocuments = docCount != null ? docCount : 0;

        EntityManager em = null;
        try {
            em = JPAUtil.getEntityManagerFactory().createEntityManager();
            em.getTransaction().begin();

            // Επαληθεύσεις επιτυχημένες
            Query qSuccess = em.createQuery(
                    "SELECT COUNT(e) FROM TbAuditEvent e WHERE e.actionType = 'VERIFY' AND e.resultStatus = 'SUCCESS'");
            Object rSuccess = qSuccess.getSingleResult();
            verifySuccessCount = rSuccess != null ? ((Number) rSuccess).intValue() : 0;

            // Επαληθεύσεις αποτυχημένες
            Query qFail = em.createQuery(
                    "SELECT COUNT(e) FROM TbAuditEvent e WHERE e.actionType = 'VERIFY' AND e.resultStatus = 'FAILURE'");
            Object rFail = qFail.getSingleResult();
            verifyFailureCount = rFail != null ? ((Number) rFail).intValue() : 0;

            // Σύνολο audit events
            Query qTotal = em.createQuery("SELECT COUNT(e) FROM TbAuditEvent e");
            Object rTotal = qTotal.getSingleResult();
            totalAuditEvents = rTotal != null ? ((Number) rTotal).intValue() : 0;

            // Έγγραφα σταλμένα με email vs μη σταλμένα (document_file.email_sent)
            Query qEmailSent = em.createQuery("SELECT COUNT(d) FROM DocumentFile d WHERE d.emailSent = true");
            Object rEmailSent = qEmailSent.getSingleResult();
            emailSentCount = rEmailSent != null ? ((Number) rEmailSent).intValue() : 0;
            Query qEmailNotSent = em.createQuery("SELECT COUNT(d) FROM DocumentFile d WHERE d.emailSent IS NULL OR d.emailSent = false");
            Object rEmailNotSent = qEmailNotSent.getSingleResult();
            emailNotSentCount = rEmailNotSent != null ? ((Number) rEmailNotSent).intValue() : 0;

            // Έγγραφα ανά τύπο υπογραφής: ανυπόγραφα, μόνο RSA, μόνο Dilithium, υβριδικά
            Query qUnsigned = em.createQuery(
                    "SELECT COUNT(d) FROM DocumentFile d WHERE d.documentId NOT IN (SELECT s.documentId.documentId FROM DocumentSignature s)");
            Object rUnsigned = qUnsigned.getSingleResult();
            unsignedCount = rUnsigned != null ? ((Number) rUnsigned).intValue() : 0;
            Query qRsaOnly = em.createQuery(
                    "SELECT COUNT(s) FROM DocumentSignature s WHERE s.rsaSignature IS NOT NULL AND s.pqcSignature IS NULL");
            Object rRsaOnly = qRsaOnly.getSingleResult();
            rsaOnlyCount = rRsaOnly != null ? ((Number) rRsaOnly).intValue() : 0;
            Query qDilithiumOnly = em.createQuery(
                    "SELECT COUNT(s) FROM DocumentSignature s WHERE s.pqcSignature IS NOT NULL AND s.rsaSignature IS NULL");
            Object rDilithiumOnly = qDilithiumOnly.getSingleResult();
            dilithiumOnlyCount = rDilithiumOnly != null ? ((Number) rDilithiumOnly).intValue() : 0;
            Query qHybrid = em.createQuery(
                    "SELECT COUNT(s) FROM DocumentSignature s WHERE s.rsaSignature IS NOT NULL AND s.pqcSignature IS NOT NULL");
            Object rHybrid = qHybrid.getSingleResult();
            hybridCount = rHybrid != null ? ((Number) rHybrid).intValue() : 0;

            // Events ανά τύπο ενέργειας
            uploadCount = countByAction(em, DocumentAuditService.ACTION_UPLOAD);
            signCount = countByAction(em, DocumentAuditService.ACTION_SIGN);
            verifyCount = countByAction(em, DocumentAuditService.ACTION_VERIFY);
            emailSentCountAudit = countByAction(em, DocumentAuditService.ACTION_EMAIL_SENT);
            downloadCount = countByAction(em, DocumentAuditService.ACTION_DOWNLOAD);
            totalDownloads = downloadCount;
            int verifyTotal = verifySuccessCount + verifyFailureCount;
            verifySuccessRatePercent = verifyTotal > 0
                    ? (100.0 * verifySuccessCount / verifyTotal) : -1;

            em.getTransaction().commit();
        } catch (Exception e) {
            if (em != null && em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            totalDocuments = 0;
            verifySuccessCount = 0;
            verifyFailureCount = 0;
            totalAuditEvents = 0;
            emailSentCount = 0;
            emailNotSentCount = 0;
            unsignedCount = 0;
            rsaOnlyCount = 0;
            dilithiumOnlyCount = 0;
            hybridCount = 0;
            uploadCount = 0;
            signCount = 0;
            verifyCount = 0;
            emailSentCountAudit = 0;
            downloadCount = 0;
            totalDownloads = 0;
            verifySuccessRatePercent = -1;
        } finally {
            if (em != null) {
                em.close();
            }
        }

        // Events ανά ημέρα (native SQL για GROUP BY DATE)
        eventsPerDay = new ArrayList<>();
        try {
            String sqlDay = "SELECT DATE(CREATED_AT) as d, COUNT(*) as cnt FROM tb_audit_event GROUP BY DATE(CREATED_AT) ORDER BY d DESC LIMIT 30";
            List<?> rows = dbTransactions.getObjectsBySqlQry(sqlDay);
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            if (rows != null) {
                for (Object row : rows) {
                    Object[] arr = (Object[]) row;
                    Object dateVal = arr[0];
                    Number cnt = (Number) arr[1];
                    String label;
                    if (dateVal instanceof Date) {
                        label = sdf.format((Date) dateVal);
                    } else if (dateVal instanceof LocalDate) {
                        label = ((LocalDate) dateVal).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                    } else {
                        label = dateVal != null ? String.valueOf(dateVal) : "";
                    }
                    eventsPerDay.add(new EventCountRow(label, cnt.longValue()));
                }
                // Το SQL επιστρέφει τις πιο πρόσφατες ημερομηνίες πρώτα (DESC).
                // Αναστρέφουμε τη λίστα ώστε στον άξονα X η μικρότερη ημερομηνία να είναι αριστερά.
                Collections.reverse(eventsPerDay);
            }
        } catch (Exception e) {
            eventsPerDay = new ArrayList<>();
        }

        // Events ανά χρήστη
        eventsPerUser = new ArrayList<>();
        try {
            String sqlUser = "SELECT USERNAME, COUNT(*) as cnt FROM tb_audit_event GROUP BY USERNAME ORDER BY cnt DESC LIMIT 20";
            List<?> rows = dbTransactions.getObjectsBySqlQry(sqlUser);
            if (rows != null) {
                for (Object row : rows) {
                    Object[] arr = (Object[]) row;
                    String username = arr[0] != null ? arr[0].toString() : "";
                    Number cnt = (Number) arr[1];
                    eventsPerUser.add(new EventCountRow(username, cnt.longValue()));
                }
            }
        } catch (Exception e) {
            eventsPerUser = new ArrayList<>();
        }
    }

    private int countByAction(EntityManager em, String actionType) {
        try {
            Query q = em.createQuery("SELECT COUNT(e) FROM TbAuditEvent e WHERE e.actionType = :at");
            q.setParameter("at", actionType);
            Object r = q.getSingleResult();
            return r != null ? ((Number) r).intValue() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * JSON για Highcharts: column chart "Events ανά ημέρα".
     * Μορφή: {"categories":["dd/MM/yyyy",...],"data":[1,2,...]}
     */
    public String getEventsPerDayChartJson() {
        List<String> categories = new ArrayList<>();
        List<Long> data = new ArrayList<>();
        for (EventCountRow row : eventsPerDay) {
            categories.add(escapeJsonString(row.getLabel()));
            data.add(row.getCount());
        }
        return buildChartJson(categories, data);
    }

    /**
     * JSON για Highcharts: pie/donut "Έγγραφα σταλμένα vs μη σταλμένα".
     */
    public String getEmailSentPieChartJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"data\":[");
        sb.append("{\"name\":\"").append("Σταλμένα").append("\",\"y\":").append(emailSentCount).append(",\"color\":\"#22b165\"},");
        sb.append("{\"name\":\"").append("Μη σταλμένα").append("\",\"y\":").append(emailNotSentCount).append(",\"color\":\"#cb2426\"}");
        sb.append("]}");
        return sb.toString();
    }

    /**
     * JSON για Highcharts: pie/donut "Έγγραφα ανά τύπο υπογραφής" (ανυπόγραφα, RSA, Dilithium, υβριδικά).
     */
    public String getSignaturePieChartJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"data\":[");
        sb.append("{\"name\":\"").append("Ανυπόγραφα").append("\",\"y\":").append(unsignedCount).append(",\"color\":\"#cb2426\"},");
        sb.append("{\"name\":\"").append("RSA").append("\",\"y\":").append(rsaOnlyCount).append(",\"color\":\"#3d72b4\"},");
        sb.append("{\"name\":\"").append("Dilithium").append("\",\"y\":").append(dilithiumOnlyCount).append(",\"color\":\"#e65100\"},");
        sb.append("{\"name\":\"").append("Υβριδικά").append("\",\"y\":").append(hybridCount).append(",\"color\":\"#22b165\"}");
        sb.append("]}");
        return sb.toString();
    }

    /**
     * JSON για Highcharts: pie "Events ανά τύπο ενέργειας" (UPLOAD, SIGN, VERIFY, EMAIL_SENT, DOWNLOAD).
     */
    public String getActionTypePieChartJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"data\":[");
        sb.append("{\"name\":\"").append("Upload").append("\",\"y\":").append(uploadCount).append(",\"color\":\"#3d72b4\"},");
        sb.append("{\"name\":\"").append("Υπογραφή").append("\",\"y\":").append(signCount).append(",\"color\":\"#e65100\"},");
        sb.append("{\"name\":\"").append("Επαλήθευση").append("\",\"y\":").append(verifyCount).append(",\"color\":\"#22b165\"},");
        sb.append("{\"name\":\"").append("Email").append("\",\"y\":").append(emailSentCountAudit).append(",\"color\":\"#9c27b0\"},");
        sb.append("{\"name\":\"").append("Λήψη").append("\",\"y\":").append(downloadCount).append(",\"color\":\"#607d8b\"}");
        sb.append("]}");
        return sb.toString();
    }

    /**
     * JSON για Highcharts: bar "Events ανά χρήστη".
     */
    public String getEventsPerUserChartJson() {
        List<String> categories = new ArrayList<>();
        List<Long> data = new ArrayList<>();
        for (EventCountRow row : eventsPerUser) {
            categories.add(escapeJsonString(row.getLabel()));
            data.add(row.getCount());
        }
        return buildChartJson(categories, data);
    }

    private static String escapeJsonString(String s) {
        if (s == null) return "\"\"";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\"";
    }

    private static String buildChartJson(List<String> categories, List<Long> data) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"categories\":[");
        for (int i = 0; i < categories.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(categories.get(i));
        }
        sb.append("],\"data\":[");
        for (int i = 0; i < data.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(data.get(i));
        }
        sb.append("]}");
        return sb.toString();
    }

    // --- Getters (για τη view) ---

    public int getTotalDocuments() { return totalDocuments; }
    public int getVerifySuccessCount() { return verifySuccessCount; }
    public int getVerifyFailureCount() { return verifyFailureCount; }
    public int getTotalAuditEvents() { return totalAuditEvents; }
    public int getEmailSentCount() { return emailSentCount; }
    public int getEmailNotSentCount() { return emailNotSentCount; }
    public int getUnsignedCount() { return unsignedCount; }
    public int getRsaOnlyCount() { return rsaOnlyCount; }
    public int getDilithiumOnlyCount() { return dilithiumOnlyCount; }
    public int getHybridCount() { return hybridCount; }
    public int getUploadCount() { return uploadCount; }
    public int getSignCount() { return signCount; }
    public int getVerifyCount() { return verifyCount; }
    public int getEmailSentCountAudit() { return emailSentCountAudit; }
    public int getDownloadCount() { return downloadCount; }
    public int getTotalDownloads() { return totalDownloads; }
    public double getVerifySuccessRatePercent() { return verifySuccessRatePercent; }

    /** Για την οθόνη: "85,2%" ή "—" αν δεν υπάρχουν επαληθεύσεις. */
    public String getVerifySuccessRateDisplay() {
        if (verifySuccessRatePercent < 0) return "—";
        DecimalFormat df = new DecimalFormat("0.0", new DecimalFormatSymbols(Locale.forLanguageTag("el-GR")));
        return df.format(verifySuccessRatePercent) + "%";
    }

    public List<EventCountRow> getEventsPerDay() { return eventsPerDay; }
    public List<EventCountRow> getEventsPerUser() { return eventsPerUser; }
}
