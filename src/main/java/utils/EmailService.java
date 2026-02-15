package utils;

import db.dbTransactions;
import model.DocumentFile;
import model.DocumentSignature;
import model.ParkingBooking;
import org.simplejavamail.api.email.Email;
import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.mailer.MailerBuilder;
import org.apache.log4j.Logger;

import java.io.Serializable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Utility class για αποστολή emails με υπογεγραμμένα PDFs.
 * Χρησιμοποιεί την ίδια SMTP configuration όπως το kratisiThesisBean.
 * Implements Serializable so it can be injected into passivating scoped beans (e.g. @ViewScoped).
 */
public class EmailService implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger log = Logger.getLogger(EmailService.class);

    // SMTP Configuration - ίδια με το kratisiThesisBean
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final int SMTP_PORT = 587;
    private static final String SMTP_USERNAME = "stamags1988@gmail.com";
    private static final String SMTP_PASSWORD = "yobfxwipagewywxi"; // Gmail app password
    private static final String FROM_NAME = "PQ Signatures System";
    private static final String FROM_EMAIL = "stamags1988@gmail.com";

    /**
     * Στέλνει ένα υπογεγραμμένο PDF μέσω email.
     *
     * @param recipientEmail Το email του παραλήπτη
     * @param docID          Το ID από το  DocumentFile
     * @param customMessage  Προαιρετικό μήνυμα για το email (μπορεί να είναι null)
     * @return true αν η αποστολή ήταν επιτυχής, false διαφορετικά
     */
    public static boolean sendSignedPdf(String recipientEmail,
                                        Long docID,
                                        String customMessage) {
        try {
            // Ανάγνωση του PDF αρχείου από το filesystem
            DocumentFile documentFile = db.dbTransactions.getObjectById(DocumentFile.class, docID);

            String signatureQuery = "from document_signature e where e.document_id=" + docID + ";";
            List<DocumentSignature> signatureList = (List<DocumentSignature>) (List<?>) dbTransactions.getObjectsBySqlQuery(DocumentSignature.class, signatureQuery, null, null, null);
            DocumentSignature signature = signatureList.get(0);
            Path filePath = FileStorageService.getFilePath(documentFile.getDocumentId());


            if (!Files.exists(filePath)) {
                log.error("PDF file not found for document ID: " + documentFile.getDocumentId());
                return false;
            }

            byte[] pdfBytes = Files.readAllBytes(filePath);

            // Προετοιμασία του email subject
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            String signDate = signature.getSignTime() != null ?
                    sdf.format(signature.getSignTime()) : "N/A";
            String subject = String.format("Υπογεγραμμένο Έγγραφο: %s (Υπογράφηκε: %s)",
                    documentFile.getFilename(), signDate);

            // Προετοιμασία του email body
            String plainText = buildPlainTextBody(documentFile, signature, customMessage);
            String htmlText = buildHtmlBody(documentFile, signature, customMessage);

            // Δημιουργία email με attachment
            Email email = EmailBuilder.startingBlank()
                    .from(FROM_NAME, FROM_EMAIL)
                    .to(recipientEmail)
                    .withSubject(subject)
                    .withPlainText(plainText)
                    .withHTMLText(htmlText)
                    .withAttachment(documentFile.getFilename(), pdfBytes, "application/pdf")
                    .buildEmail();

            // Δημιουργία mailer και αποστολή
            Mailer mailer = MailerBuilder
                    .withSMTPServer(SMTP_HOST, SMTP_PORT, SMTP_USERNAME, SMTP_PASSWORD)
                    .withDebugLogging(false) // Set to true για debugging αν χρειάζεται
                    .buildMailer();

            mailer.sendMail(email);
            DocumentAuditService.recordEvent(docID, DocumentAuditService.ACTION_EMAIL_SENT, DocumentAuditService.STATUS_SUCCESS);
            log.info("Email sent successfully to: " + recipientEmail + " for document: " + documentFile.getDocumentId());
            return true;

        } catch (Exception e) {
            DocumentAuditService.recordEvent(docID, DocumentAuditService.ACTION_EMAIL_SENT, DocumentAuditService.STATUS_FAILURE);
            log.error("Failed to send email to: " + recipientEmail, e);
            return false;
        }
    }

    /**
     * Δημιουργεί το plain text body του email.
     */
    private static String buildPlainTextBody(DocumentFile documentFile, DocumentSignature signature,
                                             String customMessage) {
        StringBuilder text = new StringBuilder();
        text.append("Υπογεγραμμένο Έγγραφο\n");
        text.append("====================\n\n");

        if (customMessage != null && !customMessage.trim().isEmpty()) {
            text.append(customMessage).append("\n\n");
        }

        text.append("Στοιχεία Εγγράφου:\n");
        text.append("-------------------\n");
        text.append("Όνομα Αρχείου: ").append(documentFile.getFilename()).append("\n");
        text.append("Μέγεθος: ").append(documentFile.getFileSize() != null ? documentFile.getFileSize() + " bytes" : "N/A").append("\n");
        text.append("Σχήμα Υπογραφής: ").append(signature.getScheme() != null ? signature.getScheme() : "N/A").append("\n");

        if (signature.getSignTime() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            text.append("Ημερομηνία Υπογραφής: ").append(sdf.format(signature.getSignTime())).append("\n");
        }

        text.append("\nΤο PDF είναι συνημμένο σε αυτό το email.\n");
        text.append("Μπορείτε να επαληθεύσετε την υπογραφή χρησιμοποιώντας το σύστημα επαλήθευσης.");

        return text.toString();
    }

    /**
     * Δημιουργεί το HTML body του email.
     */
    private static String buildHtmlBody(DocumentFile documentFile, DocumentSignature signature,
                                        String customMessage) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='font-family: Arial, sans-serif;'>");
        html.append("<h2 style='color: #2c3e50;'>Υπογεγραμμένο Έγγραφο</h2>");

        if (customMessage != null && !customMessage.trim().isEmpty()) {
            html.append("<p style='background-color: #ecf0f1; padding: 10px; border-radius: 5px;'>");
            html.append(customMessage.replace("\n", "<br/>"));
            html.append("</p>");
        }

        html.append("<div style='margin-top: 20px;'>");
        html.append("<h3>Στοιχεία Εγγράφου:</h3>");
        html.append("<table style='border-collapse: collapse; width: 100%;'>");

        html.append("<tr><td style='padding: 8px; font-weight: bold; border-bottom: 1px solid #ddd;'>Όνομα Αρχείου:</td>");
        html.append("<td style='padding: 8px; border-bottom: 1px solid #ddd;'>").append(documentFile.getFilename()).append("</td></tr>");

        html.append("<tr><td style='padding: 8px; font-weight: bold; border-bottom: 1px solid #ddd;'>Μέγεθος:</td>");
        html.append("<td style='padding: 8px; border-bottom: 1px solid #ddd;'>")
                .append(documentFile.getFileSize() != null ? documentFile.getFileSize() + " bytes" : "N/A")
                .append("</td></tr>");

        html.append("<tr><td style='padding: 8px; font-weight: bold; border-bottom: 1px solid #ddd;'>Σχήμα Υπογραφής:</td>");
        html.append("<td style='padding: 8px; border-bottom: 1px solid #ddd;'>")
                .append(signature.getScheme() != null ? signature.getScheme() : "N/A")
                .append("</td></tr>");

        if (signature.getSignTime() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            html.append("<tr><td style='padding: 8px; font-weight: bold; border-bottom: 1px solid #ddd;'>Ημερομηνία Υπογραφής:</td>");
            html.append("<td style='padding: 8px; border-bottom: 1px solid #ddd;'>").append(sdf.format(signature.getSignTime())).append("</td></tr>");
        }

        html.append("</table>");
        html.append("</div>");

        html.append("<div style='margin-top: 20px; padding: 15px; background-color: #e8f5e9; border-radius: 5px;'>");
        html.append("<p><strong>Το PDF είναι συνημμένο σε αυτό το email.</strong></p>");
        html.append("<p>Μπορείτε να επαληθεύσετε την υπογραφή χρησιμοποιώντας το σύστημα επαλήθευσης.</p>");
        html.append("</div>");

        html.append("</body></html>");
        return html.toString();
    }

    /**
     * Έλεγχος αν το email είναι valid format.
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        // Απλός regex έλεγχος για email format
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }
}
