package utils;

import model.DocumentSignature;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationText;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.List;

/**
 * Utility class για την ενσωμάτωση υπογραφών σε αρχεία PDF.
 * Οι υπογραφές ενσωματώνονται ως Base64-encoded strings στα custom metadata του PDF.
 */
public class PdfSignatureEmbedder {

    private static final String METADATA_KEY_RSA_SIGNATURE = "RSA_SIGNATURE";
    private static final String METADATA_KEY_PQC_SIGNATURE = "DILITHIUM_SIGNATURE";
    private static final String METADATA_KEY_SCHEME = "SIGNATURE_SCHEME";
    private static final String METADATA_KEY_SIGN_TIME = "SIGNATURE_TIME";

    /**
     * Ενσωματώνει μια υπογραφή σε ένα αρχείο PDF.
     * Δημιουργεί μια σωστή PDF cryptographic signature (PAdES) με RSA αν είναι διαθέσιμη,
     * και επίσης ενσωματώνει custom metadata για DILITHIUM και hybrid schemes.
     *
     * @param pdfPath Διαδρομή προς το αρχείο PDF
     * @param signature Η υπογραφή προς ενσωμάτωση
     * @throws Exception αν η ενσωμάτωση αποτύχει
     */
    public static void embedSignature(String pdfPath, DocumentSignature signature) throws Exception {
        // Πρώτα, προσθέτουμε custom metadata και annotations
        try (PDDocument doc = PDDocument.load(new File(pdfPath))) {
            PDDocumentInformation info = doc.getDocumentInformation();

            // Ενσωμάτωση σχήματος υπογραφής
            if (signature.getScheme() != null) {
                info.setCustomMetadataValue(METADATA_KEY_SCHEME, signature.getScheme());
            }

            // Ενσωμάτωση RSA υπογραφής (Base64 encoded) - για σκοπούς επαλήθευσης
            if (signature.getRsaSignature() != null && signature.getRsaSignature().length > 0) {
                String rsaBase64 = Base64.getEncoder().encodeToString(signature.getRsaSignature());
                info.setCustomMetadataValue(METADATA_KEY_RSA_SIGNATURE, rsaBase64);
            }

            // Ενσωμάτωση PQC/DILITHIUM υπογραφής (Base64 encoded)
            if (signature.getPqcSignature() != null && signature.getPqcSignature().length > 0) {
                String pqcBase64 = Base64.getEncoder().encodeToString(signature.getPqcSignature());
                info.setCustomMetadataValue(METADATA_KEY_PQC_SIGNATURE, pqcBase64);
            }

            // Ενσωμάτωση χρόνου υπογραφής
            if (signature.getSignTime() != null) {
                info.setCustomMetadataValue(METADATA_KEY_SIGN_TIME,
                        String.valueOf(signature.getSignTime().getTime()));
            }

            doc.setDocumentInformation(info);

            // Προσθήκη οπτικού signature annotation
            addVisibleSignatureAnnotation(doc, signature);

            doc.save(pdfPath);
        }

        // Τώρα προσθέτουμε σωστή PDF cryptographic signature αν υπάρχει RSA υπογραφή
        if (signature.getRsaSignature() != null && signature.getRsaSignature().length > 0) {
            addPdfCryptographicSignature(pdfPath, signature);
        }
    }

    /**
     * Προσθέτει μια σωστή PDF cryptographic signature (PAdES compatible) στο PDF.
     * Αυτό δημιουργεί μια υπογραφή που το Adobe Reader θα αναγνωρίσει ως "Signed".
     * Χρησιμοποιεί PDFBox 2.0.14 API με external signing approach.
     *
     * @param pdfPath Διαδρομή προς το αρχείο PDF
     * @param signature Οι πληροφορίες της υπογραφής
     * @throws Exception αν η υπογραφή αποτύχει
     */
    private static void addPdfCryptographicSignature(String pdfPath, DocumentSignature signature) throws Exception {
        // Δημιουργία προσωρινού αρχείου για incremental save
        Path tempPath = Files.createTempFile("pdf_sign_", ".pdf");
        Path originalPath = Paths.get(pdfPath);

        try {
            // Φόρτωση εγγράφου και προετοιμασία για υπογραφή
            PDDocument doc = PDDocument.load(new File(pdfPath));

            try {
                // Λήψη της τελευταίας σελίδας για εμφάνιση υπογραφής
                int pageCount = doc.getNumberOfPages();
                if (pageCount == 0) {
                    throw new Exception("Το PDF δεν έχει σελίδες");
                }

                // Δημιουργία signature dictionary - ΠΡΕΠΕΙ να δημιουργηθεί πριν την υπογραφή
                PDSignature pdSignature = new PDSignature();

                // Ορισμός ιδιοτήτων υπογραφής
                Calendar cal = Calendar.getInstance();
                if (signature.getSignTime() != null) {
                    cal.setTime(signature.getSignTime());
                }
                pdSignature.setSignDate(cal);

                // Ορισμός signature filter και subfilter για PAdES
                pdSignature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
                pdSignature.setSubFilter(PDSignature.SUBFILTER_ETSI_CADES_DETACHED);

                // Ορισμός ονόματος υπογραφής
                String signerName = "PQ-Signatures System";
                if (signature.getScheme() != null) {
                    signerName += " (" + signature.getScheme() + ")";
                }
                pdSignature.setName(signerName);
                pdSignature.setReason("Digital Signature");
                pdSignature.setLocation("Greece");

                // Προσθήκη signature dictionary στο έγγραφο (χωρίς SignatureInterface)
                // Αυτό προετοιμάζει το PDF για external signing
                doc.addSignature(pdSignature, (SignatureInterface) null);

                // Χρήση external signing - αυτή είναι η σωστή προσέγγιση για PDFBox 2.0.14
                try (FileOutputStream output = new FileOutputStream(tempPath.toFile())) {
                    // Δημιουργία external signing container - αυτό προετοιμάζει το PDF για υπογραφή
                    // Θα δημιουργήσει αυτόματα το signature field και θα ρυθμίσει το byteRange
                    org.apache.pdfbox.pdmodel.interactive.digitalsignature.ExternalSigningSupport externalSigning =
                            doc.saveIncrementalForExternalSigning(output);

                    // Λήψη περιεχομένου προς υπογραφή (επιστρέφει InputStream)
                    java.io.InputStream contentToSign = externalSigning.getContent();

                    // Υπογραφή περιεχομένου χρησιμοποιώντας το signature interface μας
                    // Αυτό επιστρέφει πλήρες CMS/PKCS#7 SignedData με certificate
                    RsaSignatureInterface signatureInterface = new RsaSignatureInterface();
                    byte[] signatureBytes = signatureInterface.sign(contentToSign);

                    // Ορισμός των signature bytes πίσω - αυτό ολοκληρώνει την υπογραφή και ορίζει το byteRange
                    externalSigning.setSignature(signatureBytes);

                } catch (Exception e) {
                    // Αν το external signing αποτύχει, καταγράφουμε και συνεχίζουμε χωρίς PDF signature
                    System.err.println("Warning: Δεν ήταν δυνατή η δημιουργία PDF cryptographic signature: " + e.getMessage());
                    System.err.println("Το PDF θα έχει custom metadata signatures αλλά μπορεί να μην εμφανίζεται ως 'Signed' στο Adobe Reader");
                    e.printStackTrace();
                    // Μην πετάξουμε exception - επιτρέπουμε τη συνέχιση με metadata-only signatures
                    Files.deleteIfExists(tempPath);
                    return; // Έξοδος νωρίς, κρατάμε το αρχικό αρχείο
                }
            } finally {
                // ΚΡΙΣΙΜΟ: Κλείσιμο εγγράφου ΠΡΙΝ από την προσπάθεια αντικατάστασης του αρχείου
                doc.close();
            }

            // Τώρα που το έγγραφο είναι κλειστό, αντικαθιστούμε το αρχικό αρχείο με την υπογεγραμμένη έκδοση
            Files.move(tempPath, originalPath, StandardCopyOption.REPLACE_EXISTING);

        } catch (Exception e) {
            // Καθαρισμός προσωρινού αρχείου σε περίπτωση σφάλματος
            try {
                Files.deleteIfExists(tempPath);
            } catch (IOException ignored) {}
            // Μην πετάξουμε exception - επιτρέπουμε τη λειτουργία metadata-only signatures
            System.err.println("Warning: Η PDF cryptographic signature απέτυχε: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Προσθέτει ένα οπτικό signature annotation στο PDF έγγραφο.
     * Το annotation εμφανίζεται στην τελευταία σελίδα του εγγράφου.
     *
     * @param doc Το PDF έγγραφο
     * @param signature Οι πληροφορίες της υπογραφής
     * @throws IOException αν η προσθήκη annotation αποτύχει
     */
    private static void addVisibleSignatureAnnotation(PDDocument doc, DocumentSignature signature) throws IOException {
        int pageCount = doc.getNumberOfPages();
        if (pageCount == 0) {
            return; // Δεν υπάρχουν σελίδες για προσθήκη annotation
        }

        PDPage page = doc.getPage(pageCount - 1); // Τελευταία σελίδα
        PDRectangle pageSize = page.getMediaBox();

        // Δημιουργία signature text annotation
        PDAnnotationText annotation = new PDAnnotationText();
        annotation.setAnnotationName("DigitalSignature");

        // Κατασκευή κειμένου υπογραφής
        StringBuilder sigText = new StringBuilder();
        sigText.append("ΨΗΦΙΑΚΗ ΥΠΟΓΡΑΦΗ\n");
        sigText.append("==================\n\n");

        if (signature.getScheme() != null) {
            sigText.append("Σχήμα: ").append(signature.getScheme()).append("\n");
        }

        if (signature.getRsaSignature() != null && signature.getRsaSignature().length > 0) {
            sigText.append("RSA: ✓ Υπογεγραμμένο\n");
        }

        if (signature.getPqcSignature() != null && signature.getPqcSignature().length > 0) {
            sigText.append("DILITHIUM: ✓ Υπογεγραμμένο\n");
        }

        if (signature.getSignTime() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            sigText.append("Ημερομηνία: ").append(sdf.format(signature.getSignTime())).append("\n");
        }

        annotation.setContents(sigText.toString());
        annotation.setSubject("Ψηφιακή Υπογραφή");

        // Τοποθέτηση annotation στην κάτω δεξιά γωνία της σελίδας
        float width = 200;
        float height = 100;
        float x = pageSize.getWidth() - width - 20; // 20 points από το δεξί άκρο
        float y = 20; // 20 points από το κάτω μέρος

        PDRectangle rect = new PDRectangle(x, y, width, height);
        annotation.setRectangle(rect);

        // Ορισμός εμφάνισης (προαιρετικό, για καλύτερη ορατότητα)
        annotation.setOpen(false);
        annotation.setColor(new org.apache.pdfbox.pdmodel.graphics.color.PDColor(
                new float[]{0.0f, 0.5f, 0.0f}, // Πράσινο χρώμα
                org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB.INSTANCE
        ));

        // Προσθήκη annotation στη σελίδα
        List<PDAnnotation> annotations = page.getAnnotations();
        if (annotations == null) {
            annotations = new ArrayList<>();
            page.setAnnotations(annotations);
        }
        annotations.add(annotation);

        // ΣΗΜΕΙΩΣΗ: ΔΕΝ δημιουργούμε PDSignatureField εδώ γιατί θα δημιουργηθεί από το addPdfCryptographicSignature
        // με σωστή υπογραφή. Αν δημιουργήσουμε ένα unsigned field εδώ, θα εμφανίζεται ως unsigned στο PDF.
    }

    /**
     * Ενσωματώνει μια υπογραφή σε ένα αρχείο PDF χρησιμοποιώντας document ID.
     * Διαβάζει τη διαδρομή αρχείου από το FileStorageService.
     *
     * @param documentId Το document ID
     * @param signature Η υπογραφή προς ενσωμάτωση
     * @throws Exception αν η ενσωμάτωση αποτύχει
     */
    public static void embedSignature(Long documentId, DocumentSignature signature) throws Exception {
        Path filePath = FileStorageService.getFilePath(documentId);
        if (!Files.exists(filePath)) {
            throw new Exception("PDF file not found for document ID: " + documentId);
        }
        embedSignature(filePath.toString(), signature);
    }

    /**
     * Διαβάζει ενσωματωμένες υπογραφές από ένα αρχείο PDF.
     *
     * @param pdfPath Διαδρομή προς το αρχείο PDF
     * @return DocumentSignature με ενσωματωμένα δεδομένα, ή null αν δεν βρεθούν υπογραφές
     * @throws Exception αν η ανάγνωση αποτύχει
     */
    public static DocumentSignature readEmbeddedSignature(String pdfPath) throws Exception {
        try (PDDocument doc = PDDocument.load(new File(pdfPath))) {
            PDDocumentInformation info = doc.getDocumentInformation();

            String scheme = info.getCustomMetadataValue(METADATA_KEY_SCHEME);
            String rsaBase64 = info.getCustomMetadataValue(METADATA_KEY_RSA_SIGNATURE);
            String pqcBase64 = info.getCustomMetadataValue(METADATA_KEY_PQC_SIGNATURE);
            String signTimeStr = info.getCustomMetadataValue(METADATA_KEY_SIGN_TIME);

            // Αν δεν βρεθούν υπογραφές, επιστροφή null
            if (scheme == null && rsaBase64 == null && pqcBase64 == null) {
                return null;
            }

            DocumentSignature sig = new DocumentSignature();
            sig.setScheme(scheme);

            if (rsaBase64 != null && !rsaBase64.isEmpty()) {
                byte[] rsaBytes = Base64.getDecoder().decode(rsaBase64);
                sig.setRsaSignature(rsaBytes);
            }

            if (pqcBase64 != null && !pqcBase64.isEmpty()) {
                byte[] pqcBytes = Base64.getDecoder().decode(pqcBase64);
                sig.setPqcSignature(pqcBytes);
            }

            if (signTimeStr != null && !signTimeStr.isEmpty()) {
                try {
                    long timestamp = Long.parseLong(signTimeStr);
                    sig.setSignTime(new java.util.Date(timestamp));
                } catch (NumberFormatException e) {
                    // Αγνόηση μη έγκυρου timestamp
                }
            }

            return sig;
        }
    }

    /**
     * Διαβάζει ενσωματωμένες υπογραφές από ένα αρχείο PDF χρησιμοποιώντας document ID.
     *
     * @param documentId Το document ID
     * @return DocumentSignature με ενσωματωμένα δεδομένα, ή null αν δεν βρεθούν υπογραφές
     * @throws Exception αν η ανάγνωση αποτύχει
     */
    public static DocumentSignature readEmbeddedSignature(Long documentId) throws Exception {
        Path filePath = FileStorageService.getFilePath(documentId);
        if (!Files.exists(filePath)) {
            throw new Exception("PDF file not found for document ID: " + documentId);
        }
        return readEmbeddedSignature(filePath.toString());
    }

    /**
     * Ελέγχει αν ένα αρχείο PDF έχει ενσωματωμένες υπογραφές.
     *
     * @param pdfPath Διαδρομή προς το αρχείο PDF
     * @return true αν υπάρχουν ενσωματωμένες υπογραφές, false διαφορετικά
     */
    public static boolean hasEmbeddedSignature(String pdfPath) {
        try {
            DocumentSignature sig = readEmbeddedSignature(pdfPath);
            return sig != null && (sig.getRsaSignature() != null || sig.getPqcSignature() != null);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Ελέγχει αν ένα αρχείο PDF έχει ενσωματωμένες υπογραφές χρησιμοποιώντας document ID.
     *
     * @param documentId Το document ID
     * @return true αν υπάρχουν ενσωματωμένες υπογραφές, false διαφορετικά
     */
    public static boolean hasEmbeddedSignature(Long documentId) {
        try {
            return hasEmbeddedSignature(FileStorageService.getFilePath(documentId).toString());
        } catch (Exception e) {
            return false;
        }
    }
}
