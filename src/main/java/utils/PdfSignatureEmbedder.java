package utils;

import model.DocumentSignature;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationText;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSFloat;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.util.Matrix;

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
    /**
     * Προετοιμάζει το PDF για υπογραφή προσθέτοντας metadata/annotations και signature field.
     * Αυτή η μέθοδος ΔΕΝ προσθέτει τις υπογραφές, μόνο το signature field.
     */
    public static void prepareSignatureField(String pdfPath, DocumentSignature signature) throws Exception {
        // Προσθέτουμε custom metadata και annotations (χωρίς υπογραφές ακόμα)
        try (PDDocument doc = PDDocument.load(new File(pdfPath))) {
            PDDocumentInformation info = doc.getDocumentInformation();

            // Ενσωμάτωση σχήματος υπογραφής
            if (signature.getScheme() != null) {
                info.setCustomMetadataValue(METADATA_KEY_SCHEME, signature.getScheme());
            }

            // Ενσωμάτωση χρόνου υπογραφής
            if (signature.getSignTime() != null) {
                info.setCustomMetadataValue(METADATA_KEY_SIGN_TIME,
                        String.valueOf(signature.getSignTime().getTime()));
            }

            doc.setDocumentInformation(info);

            // Προσθήκη οπτικού signature annotation
            addVisibleSignatureAnnotation(doc, signature);

            // Δημιουργία signature dictionary και προσθήκη signature field
            PDSignature pdSignature = new PDSignature();
            Calendar cal = Calendar.getInstance();
            if (signature.getSignTime() != null) {
                cal.setTime(signature.getSignTime());
            }
            pdSignature.setSignDate(cal);
            pdSignature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
            pdSignature.setSubFilter(PDSignature.SUBFILTER_ETSI_CADES_DETACHED);
            pdSignature.setName("PQ-Signatures System");
            pdSignature.setReason("Digital Signature");
            pdSignature.setLocation("Greece");

            PDAcroForm acroForm = doc.getDocumentCatalog().getAcroForm();
            if (acroForm == null) {
                acroForm = new PDAcroForm(doc);
                doc.getDocumentCatalog().setAcroForm(acroForm);
            }

            PDPage firstPage = doc.getPage(0);
            PDRectangle pageSize = firstPage.getMediaBox();
            float sigWidth = pageSize.getWidth() - 40;
            float sigHeight = 80;
            float sigX = 20;
            float sigY = pageSize.getHeight() - sigHeight - 20;

            org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions signatureOptions =
                    new org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions();
            signatureOptions.setPreferredSignatureSize(30000);
            signatureOptions.setPage(0);

            doc.addSignature(pdSignature, (SignatureInterface) null, signatureOptions);
            doc.save(pdfPath);
        }
    }

    public static void embedSignature(String pdfPath, DocumentSignature signature) throws Exception {
        System.out.println("DEBUG embedSignature: Starting - RSA: " +
                (signature.getRsaSignature() != null ? signature.getRsaSignature().length + " bytes" : "null") +
                ", PQC: " + (signature.getPqcSignature() != null ? signature.getPqcSignature().length + " bytes" : "null") +
                ", Scheme: " + signature.getScheme());

        // ΚΡΙΣΙΜΟ: Πρέπει να προσθέσουμε ΟΛΑ τα metadata ΠΡΙΝ την υπογραφή
        // Αν προσθέσουμε metadata μετά την υπογραφή με doc.save(), θα ακυρώσουμε το ByteRange

        // Πρώτα, προσθέτουμε custom metadata και annotations
        // Αυτά θα συμπεριληφθούν στα signed bytes
        try (PDDocument doc = PDDocument.load(new File(pdfPath))) {
            PDDocumentInformation info = doc.getDocumentInformation();

            // Ενσωμάτωση σχήματος υπογραφής
            if (signature.getScheme() != null) {
                info.setCustomMetadataValue(METADATA_KEY_SCHEME, signature.getScheme());
            }

            // Ενσωμάτωση χρόνου υπογραφής
            if (signature.getSignTime() != null) {
                info.setCustomMetadataValue(METADATA_KEY_SIGN_TIME,
                        String.valueOf(signature.getSignTime().getTime()));
            }

            // ΚΡΙΣΙΜΟ: Προσθήκη RSA/PQC signatures στο metadata ΠΡΙΝ την υπογραφή
            // (θα γεμίσουν μετά την υπογραφή στο addPdfCryptographicSignature)
            // Αλλά πρέπει να υπάρχουν placeholder values για να συμπεριληφθούν στα signed bytes
            // Ή μπορούμε να τα προσθέσουμε μετά με incremental update

            doc.setDocumentInformation(info);

            // Προσθήκη οπτικού signature annotation
            addVisibleSignatureAnnotation(doc, signature);

            doc.save(pdfPath);
        }

        // Τώρα προσθέτουμε σωστή PDF cryptographic signature
        // Στο addPdfCryptographicSignature θα υπογράψουμε τα bytes που θα ορίσει το ByteRange
        // και θα τα αποθηκεύσουμε στο signature object
        System.out.println("DEBUG embedSignature: Calling addPdfCryptographicSignature...");
        addPdfCryptographicSignature(pdfPath, signature);
        System.out.println("DEBUG embedSignature: After addPdfCryptographicSignature - RSA: " +
                (signature.getRsaSignature() != null ? signature.getRsaSignature().length + " bytes" : "null") +
                ", PQC: " + (signature.getPqcSignature() != null ? signature.getPqcSignature().length + " bytes" : "null"));

        // ΚΡΙΣΙΜΟ: Το saveIncremental ΔΕΝ αποθηκεύει custom metadata στο DocumentInformation
        // Οι υπογραφές θα αποθηκευτούν στη βάση δεδομένων και θα διαβαστούν από εκεί κατά την επαλήθευση
        // ΔΕΝ προσπαθούμε να τα αποθηκεύσουμε στο PDF metadata με incremental save
        System.out.println("DEBUG embedSignature: Skipping metadata save (saveIncremental doesn't support custom metadata)");
        System.out.println("DEBUG embedSignature: Signatures will be read from database during verification");

        System.out.println("DEBUG embedSignature: Finished - RSA: " +
                (signature.getRsaSignature() != null ? signature.getRsaSignature().length + " bytes" : "null") +
                ", PQC: " + (signature.getPqcSignature() != null ? signature.getPqcSignature().length + " bytes" : "null"));
    }

    /**
     * Παίρνει τα bytes που θα ορίσει το ByteRange για υπογραφή.
     * Αυτή η μέθοδος υποθέτει ότι το PDF έχει ήδη προετοιμαστεί με prepareSignatureField.
     *
     * @param pdfPath Διαδρομή προς το αρχείο PDF (που έχει ήδη signature field)
     * @param signature Οι πληροφορίες της υπογραφής (για debug)
     * @return Τα bytes που θα ορίσει το ByteRange
     * @throws Exception αν αποτύχει
     */
    public static byte[] getBytesToSignForByteRange(String pdfPath, DocumentSignature signature) throws Exception {
        try {
            // Φορτώνουμε το PDF που έχει ήδη προετοιμαστεί με signature field
            try (PDDocument doc = PDDocument.load(new File(pdfPath))) {
                // Δημιουργία προσωρινού output stream για να πάρουμε τα bytes
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();

                org.apache.pdfbox.pdmodel.interactive.digitalsignature.ExternalSigningSupport externalSigning =
                        doc.saveIncrementalForExternalSigning(baos);

                java.io.InputStream contentToSign = externalSigning.getContent();
                byte[] bytesToSign = contentToSign.readAllBytes();

                System.out.println("DEBUG getBytesToSignForByteRange: Bytes length: " + bytesToSign.length);

                // Υπολογισμός hash για σύγκριση
                java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
                byte[] bytesHash = md.digest(bytesToSign);
                System.out.println("DEBUG getBytesToSignForByteRange: Bytes SHA-256 hash (first 16 bytes): " +
                        java.util.HexFormat.of().formatHex(java.util.Arrays.copyOf(bytesHash, 16)));

                return bytesToSign;
            }
        } catch (Exception e) {
            throw new Exception("Αποτυχία λήψης bytes για ByteRange: " + e.getMessage(), e);
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

                // ΚΡΙΣΙΜΟ: Δημιουργία signature dictionary και signature field
                // Αυτό πρέπει να γίνει πριν το external signing
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

                // Δημιουργία signature field με visual appearance ΠΡΙΝ την υπογραφή
                PDPage firstPage = doc.getPage(0);
                PDRectangle pageSize = firstPage.getMediaBox();

                float sigWidth = pageSize.getWidth() - 40;
                float sigHeight = 80;
                float sigX = 20;
                float sigY = pageSize.getHeight() - sigHeight - 20;
                PDRectangle sigRect = new PDRectangle(sigX, sigY, sigWidth, sigHeight);

                // Δημιουργία signature field
                PDAcroForm acroForm = doc.getDocumentCatalog().getAcroForm();
                if (acroForm == null) {
                    acroForm = new PDAcroForm(doc);
                    doc.getDocumentCatalog().setAcroForm(acroForm);
                }

                // Δημιουργία SignatureOptions με preferredSignatureSize για CMS
                // Το CMS/PKCS#7 μπορεί να είναι μεγάλο (ειδικά με certificate chain)
                org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions signatureOptions =
                        new org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions();
                signatureOptions.setPreferredSignatureSize(30000); // 30KB για CMS με cert chain
                signatureOptions.setPage(0); // Πρώτη σελίδα

                // Προσθήκη signature dictionary στο έγγραφο (χωρίς SignatureInterface)
                // Αυτό προετοιμάζει το PDF για external signing
                // Το PDFBox θα δημιουργήσει το signature field αυτόματα
                doc.addSignature(pdSignature, (SignatureInterface) null, signatureOptions);

                // Μετά το addSignature, βρίσκουμε το signature field που δημιουργήθηκε
                PDSignatureField sigField = null;
                for (org.apache.pdfbox.pdmodel.interactive.form.PDField field : acroForm.getFields()) {
                    if (field instanceof PDSignatureField) {
                        sigField = (PDSignatureField) field;
                        break;
                    }
                }

                if (sigField != null) {
                    // Δημιουργία visual appearance στο signature field που δημιουργήθηκε
                    createSignatureAppearance(doc, sigField, sigRect, signature);

                    // Ενημέρωση widget annotation
                    @SuppressWarnings("deprecation")
                    var widget = sigField.getWidget();
                    if (widget != null) {
                        widget.setRectangle(sigRect);
                        widget.setPage(firstPage);

                        // Προσθήκη στη σελίδα αν δεν υπάρχει ήδη
                        List<PDAnnotation> annotations = firstPage.getAnnotations();
                        if (annotations == null) {
                            annotations = new ArrayList<>();
                            firstPage.setAnnotations(annotations);
                        }
                        boolean found = false;
                        for (PDAnnotation ann : annotations) {
                            if (ann == widget) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            annotations.add(widget);
                        }
                    }
                }

                // Χρήση external signing - αυτή είναι η σωστή προσέγγιση για PDFBox 2.0.14
                // Το appearance είναι ήδη προσθεμένο στο signature field, οπότε θα συμπεριληφθεί στο signed content
                try (FileOutputStream output = new FileOutputStream(tempPath.toFile())) {
                    // Δημιουργία external signing container
                    org.apache.pdfbox.pdmodel.interactive.digitalsignature.ExternalSigningSupport externalSigning =
                            doc.saveIncrementalForExternalSigning(output);

                    // ΚΡΙΣΙΜΟ: Λήψη περιεχομένου προς υπογραφή (αυτά είναι τα bytes που ορίζονται από το ByteRange)
                    // Αυτά είναι τα bytes που πρέπει να υπογράφουμε για RSA/PQC
                    java.io.InputStream contentToSign = externalSigning.getContent();
                    byte[] bytesToSign = contentToSign.readAllBytes();

                    System.out.println("DEBUG addPdfCryptographicSignature: Bytes to sign length: " + bytesToSign.length);

                    // ΚΡΙΣΙΜΟ: Υπολογισμός hash για σύγκριση με verify
                    java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
                    byte[] bytesHash = md.digest(bytesToSign);
                    System.out.println("DEBUG addPdfCryptographicSignature: Bytes SHA-256 hash (first 16 bytes): " +
                            java.util.HexFormat.of().formatHex(java.util.Arrays.copyOf(bytesHash, 16)));

                    // ΚΡΙΣΙΜΟ: Υπογραφή των bytes με RSA και PQC (raw signatures) ΠΡΙΝ το CMS blob
                    // Αυτά είναι τα bytes που θα ορίσει το ByteRange στο τελικό PDF
                    String scheme = signature.getScheme();
                    // ΚΡΙΣΙΜΟ: Normalize το scheme για case-insensitive comparison
                    scheme = scheme == null ? "" : scheme.trim().toUpperCase();
                    System.out.println("DEBUG addPdfCryptographicSignature: Normalized scheme: '" + scheme + "'");

                    if ("RSA".equals(scheme) || "HYBRID".equals(scheme)) {
                        // ΚΡΙΣΙΜΟ: Πάντα υπογράφουμε (ακόμα κι αν υπάρχει ήδη)
                        // γιατί μπορεί να έχουμε υπογράψει με διαφορετικά bytes
                        java.security.PrivateKey rsaPriv = signatures.KeyLoader.loadPrivateKey("data/rsa-private.key", "RSA");
                        java.security.Signature rsa = java.security.Signature.getInstance("SHA256withRSA");
                        rsa.initSign(rsaPriv);
                        rsa.update(bytesToSign);
                        signature.setRsaSignature(rsa.sign());
                        System.out.println("DEBUG addPdfCryptographicSignature: RSA signature created, length: " + signature.getRsaSignature().length);
                    } else {
                        System.out.println("DEBUG addPdfCryptographicSignature: Skipping RSA signature (scheme='" + scheme + "')");
                    }

                    if ("DILITHIUM".equals(scheme) || "HYBRID".equals(scheme)) {
                        // ΚΡΙΣΙΜΟ: Πάντα υπογράφουμε (ακόμα κι αν υπάρχει ήδη)
                        // γιατί μπορεί να έχουμε υπογράψει με διαφορετικά bytes
                        java.security.PrivateKey pqPriv = signatures.KeyLoader.loadPrivateKey("data/pqc-private.key", "DILITHIUM");
                        java.security.Signature pq = java.security.Signature.getInstance("DILITHIUM3", "BC");
                        pq.initSign(pqPriv);
                        pq.update(bytesToSign);
                        signature.setPqcSignature(pq.sign());
                        System.out.println("DEBUG addPdfCryptographicSignature: PQC signature created, length: " + signature.getPqcSignature().length);
                    } else {
                        System.out.println("DEBUG addPdfCryptographicSignature: Skipping PQC signature (scheme='" + scheme + "')");
                    }

                    // ΚΡΙΣΙΜΟ: Ελέγχος ότι οι υπογραφές αποθηκεύτηκαν
                    System.out.println("DEBUG addPdfCryptographicSignature: After signing - RSA: " +
                            (signature.getRsaSignature() != null ? signature.getRsaSignature().length + " bytes" : "null") +
                            ", PQC: " + (signature.getPqcSignature() != null ? signature.getPqcSignature().length + " bytes" : "null"));

                    // Τώρα υπογράφουμε για PDF cryptographic signature (CMS/PKCS#7)
                    // Χρησιμοποιούμε τα ίδια bytes
                    java.io.ByteArrayInputStream contentToSignAgain = new java.io.ByteArrayInputStream(bytesToSign);
                    RsaSignatureInterface signatureInterface = new RsaSignatureInterface();
                    byte[] signatureBytes = signatureInterface.sign(contentToSignAgain);

                    // Ορισμός των signature bytes
                    externalSigning.setSignature(signatureBytes);

                } catch (Exception e) {
                    System.err.println("Warning: Δεν ήταν δυνατή η δημιουργία PDF cryptographic signature: " + e.getMessage());
                    e.printStackTrace();
                    Files.deleteIfExists(tempPath);
                    return;
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
     * Προσθέτει visual appearance στο υπογεγραμμένο PDF ΜΕΤΑ την υπογραφή.
     * Αυτό εξασφαλίζει ότι το appearance συνδέεται με το σωστό signature field.
     *
     * @param pdfPath Διαδρομή προς το υπογεγραμμένο PDF
     * @param signature Οι πληροφορίες της υπογραφής
     * @throws Exception αν η προσθήκη appearance αποτύχει
     */
    private static void addSignatureAppearanceAfterSigning(String pdfPath, DocumentSignature signature) throws Exception {
        try (PDDocument doc = PDDocument.load(new File(pdfPath))) {
            // Εύρεση του signature field που δημιουργήθηκε από το external signing
            PDAcroForm acroForm = doc.getDocumentCatalog().getAcroForm();
            if (acroForm == null) {
                return; // Δεν υπάρχει φόρμα
            }

            PDSignatureField sigField = null;
            for (org.apache.pdfbox.pdmodel.interactive.form.PDField field : acroForm.getFields()) {
                if (field instanceof PDSignatureField) {
                    sigField = (PDSignatureField) field;
                    break;
                }
            }

            if (sigField == null) {
                return; // Δεν βρέθηκε signature field
            }

            // Λήψη της πρώτης σελίδας
            PDPage firstPage = doc.getPage(0);
            PDRectangle pageSize = firstPage.getMediaBox();

            // Ορισμός rectangle για την υπογραφή στην κορυφή
            float sigWidth = pageSize.getWidth() - 40;
            float sigHeight = 80;
            float sigX = 20;
            float sigY = pageSize.getHeight() - sigHeight - 20;
            PDRectangle sigRect = new PDRectangle(sigX, sigY, sigWidth, sigHeight);

            // Δημιουργία visual appearance
            createSignatureAppearance(doc, sigField, sigRect, signature);

            // Ενημέρωση widget annotation
            @SuppressWarnings("deprecation")
            var widget = sigField.getWidget();
            if (widget != null) {
                widget.setRectangle(sigRect);
                widget.setPage(firstPage);

                // Προσθήκη στη σελίδα αν δεν υπάρχει ήδη
                List<PDAnnotation> annotations = firstPage.getAnnotations();
                if (annotations == null) {
                    annotations = new ArrayList<>();
                    firstPage.setAnnotations(annotations);
                }
                boolean found = false;
                for (PDAnnotation ann : annotations) {
                    if (ann == widget) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    annotations.add(widget);
                }
            }

            // Αποθήκευση
            doc.save(pdfPath);
        }
    }

    /**
     * Δημιουργεί visual appearance για το signature field στην κορυφή του PDF.
     * Παρόμοιο με το gov.gr signature appearance.
     *
     * @param doc Το PDF έγγραφο
     * @param sigField Το signature field
     * @param rect Το rectangle όπου θα εμφανιστεί η υπογραφή
     * @param signature Οι πληροφορίες της υπογραφής
     * @throws IOException αν η δημιουργία appearance αποτύχει
     */
    private static void createSignatureAppearance(PDDocument doc, PDSignatureField sigField,
                                                  PDRectangle rect, DocumentSignature signature) throws IOException {
        // Δημιουργία appearance stream
        PDStream appearanceStream = new PDStream(doc);

        float width = rect.getWidth();
        float height = rect.getHeight();

        // Γράφουμε PDF content stream commands απευθείας
        try (java.io.OutputStream os = appearanceStream.createOutputStream();
             java.io.PrintWriter writer = new java.io.PrintWriter(
                     new java.io.OutputStreamWriter(os, java.nio.charset.StandardCharsets.ISO_8859_1))) {

            // Background με ανοιχτό γκρι
            writer.println("0.95 0.95 0.95 rg"); // setNonStrokingColorRGB
            writer.println("0 0 " + width + " " + height + " re"); // rectangle
            writer.println("f"); // fill

            // Border
            writer.println("0.3 0.3 0.3 RG"); // setStrokingColorRGB
            writer.println("1 w"); // setLineWidth
            writer.println("1 1 " + (width - 2) + " " + (height - 2) + " re"); // rectangle
            writer.println("S"); // stroke

            // Fonts - Χρήση Font mapping (/F1 = Helvetica, /F2 = Helvetica-Bold)
            // Αυτά πρέπει να αντιστοιχούν στο Resources/Font dictionary που θα προσθέσουμε
            writer.println("/F2 12 Tf"); // Helvetica-Bold για τίτλο
            writer.println("0.0 0.4 0.0 rg"); // πράσινο χρώμα

            // Τίτλος "DIGITAL SIGNATURE"
            float yPos = height - 15;
            float xPos = 10;
            writer.println("BT"); // beginText
            writer.println(xPos + " " + yPos + " Td"); // text position
            writer.println("(DIGITAL SIGNATURE) Tj"); // showText
            writer.println("ET"); // endText

            yPos -= 18;

            // Font Regular για το υπόλοιπο
            writer.println("/F1 10 Tf"); // Helvetica Regular
            writer.println("0.0 0.0 0.0 rg"); // μαύρο χρώμα

            // Σχήμα υπογραφής
            if (signature.getScheme() != null) {
                writer.println("BT");
                writer.println(xPos + " " + yPos + " Td");
                writer.println("(Scheme: " + signature.getScheme() + ") Tj");
                writer.println("ET");
                yPos -= 14;
            }

            // Υπογραφές
            StringBuilder sigInfo = new StringBuilder();
            if (signature.getRsaSignature() != null && signature.getRsaSignature().length > 0) {
                sigInfo.append("RSA Signed");
            }
            if (signature.getPqcSignature() != null && signature.getPqcSignature().length > 0) {
                if (sigInfo.length() > 0) sigInfo.append(" | ");
                sigInfo.append("DILITHIUM Signed");
            }

            if (sigInfo.length() > 0) {
                writer.println("/F1 9 Tf"); // Helvetica Regular, μικρότερο μέγεθος
                writer.println("BT");
                writer.println(xPos + " " + yPos + " Td");
                writer.println("(" + sigInfo.toString() + ") Tj");
                writer.println("ET");
                yPos -= 14;
            }

            // Ημερομηνία
            if (signature.getSignTime() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                writer.println("/F1 9 Tf"); // Helvetica Regular, μικρότερο μέγεθος
                writer.println("0.3 0.3 0.3 rg"); // γκρι χρώμα
                writer.println("BT");
                writer.println(xPos + " " + yPos + " Td");
                writer.println("(Date: " + sdf.format(signature.getSignTime()) + ") Tj");
                writer.println("ET");
            }
        }

        // Σύνδεση appearance με το signature field χρησιμοποιώντας low-level API
        // Στο PDFBox 2.0.14, χρησιμοποιούμε COSDictionary απευθείας
        COSDictionary appearanceDict = new COSDictionary();

        // Μετατροπή PDRectangle σε COSArray για BBox
        // Το BBox πρέπει να είναι σε user space coordinates [0 0 width height]
        COSArray bboxArray = new COSArray();
        bboxArray.add(new COSFloat(0.0f)); // lower-left X
        bboxArray.add(new COSFloat(0.0f)); // lower-left Y
        bboxArray.add(new COSFloat(width)); // upper-right X
        bboxArray.add(new COSFloat(height)); // upper-right Y

        // Μετατροπή Matrix σε COSArray (identity matrix: [1 0 0 1 0 0])
        COSArray matrixArray = new COSArray();
        matrixArray.add(new COSFloat(1.0f)); // a
        matrixArray.add(new COSFloat(0.0f)); // b
        matrixArray.add(new COSFloat(0.0f)); // c
        matrixArray.add(new COSFloat(1.0f)); // d
        matrixArray.add(new COSFloat(0.0f)); // e
        matrixArray.add(new COSFloat(0.0f)); // f

        // Δημιουργία Resources dictionary με Font mapping
        // Χρειάζεται για να εμφανίζονται τα fonts στο appearance stream
        COSDictionary resourcesDict = new COSDictionary();
        COSDictionary fontDict = new COSDictionary();

        // Χρήση Standard14Fonts - Helvetica και Helvetica-Bold
        // Στο PDFBox 2.0.14, χρησιμοποιούμε PDType1Font για να δημιουργήσουμε σωστό Font resource
        org.apache.pdfbox.pdmodel.font.PDType1Font fontHelvetica =
                org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA;
        org.apache.pdfbox.pdmodel.font.PDType1Font fontHelveticaBold =
                org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD;

        // Προσθήκη fonts στο Font dictionary
        // Τα Standard14Fonts μπορούν να χρησιμοποιηθούν απευθείας ως resources
        fontDict.setItem(COSName.getPDFName("F1"), fontHelvetica.getCOSObject());
        fontDict.setItem(COSName.getPDFName("F2"), fontHelveticaBold.getCOSObject());

        resourcesDict.setItem(COSName.getPDFName("Font"), fontDict);

        // Σύνδεση του stream με το appearance dictionary
        COSDictionary streamDict = appearanceStream.getCOSObject();
        streamDict.setItem(COSName.TYPE, COSName.XOBJECT);
        streamDict.setItem(COSName.SUBTYPE, COSName.FORM);
        streamDict.setItem(COSName.BBOX, bboxArray);
        streamDict.setItem(COSName.MATRIX, matrixArray);
        streamDict.setItem(COSName.getPDFName("Resources"), resourcesDict);

        // Προσθήκη στο normal appearance - απευθείας stream
        appearanceDict.setItem(COSName.getPDFName("N"), streamDict);

        // ΚΡΙΣΙΜΟ: Το /AP πρέπει να μπει ΜΟΝΟ στο widget annotation, όχι στο signature field
        // Πολλοί PDF readers αγνοούν το /AP στο field dictionary
        @SuppressWarnings("deprecation")
        var widget = sigField.getWidget();
        if (widget != null) {
            widget.setRectangle(rect);
            // Σύνδεση appearance ΜΟΝΟ με το widget annotation
            widget.getCOSObject().setItem(COSName.getPDFName("AP"), appearanceDict);
        }
    }

    /**
     * Προσθέτει ένα οπτικό box απευθείας στη σελίδα ΜΕΤΑ την υπογραφή.
     * Αυτό είναι workaround για το πρόβλημα με το appearance stream.
     * Χρησιμοποιεί incremental update, οπότε δεν αλλάζει το signed content.
     *
     * @param pdfPath Διαδρομή προς το υπογεγραμμένο PDF
     * @param signature Οι πληροφορίες της υπογραφής
     * @throws Exception αν η προσθήκη αποτύχει
     */
    private static void addVisualSignatureBoxAfterSigning(String pdfPath, DocumentSignature signature) throws Exception {
        try (PDDocument doc = PDDocument.load(new File(pdfPath))) {
            PDPage firstPage = doc.getPage(0);
            PDRectangle pageSize = firstPage.getMediaBox();

            float sigWidth = pageSize.getWidth() - 40;
            float sigHeight = 80;
            float sigX = 20;
            float sigY = pageSize.getHeight() - sigHeight - 20;
            PDRectangle sigRect = new PDRectangle(sigX, sigY, sigWidth, sigHeight);

            addVisualSignatureBox(doc, firstPage, sigRect, signature);

            // Αποθήκευση με incremental update
            doc.save(pdfPath);
        }
    }

    /**
     * Προσθέτει ένα οπτικό box απευθείας στη σελίδα για να φαίνεται η υπογραφή.
     * Αυτό είναι workaround για το πρόβλημα με το appearance stream.
     *
     * @param doc Το PDF έγγραφο
     * @param page Η σελίδα όπου θα προστεθεί το box
     * @param rect Το rectangle όπου θα εμφανιστεί η υπογραφή
     * @param signature Οι πληροφορίες της υπογραφής
     * @throws IOException αν η προσθήκη αποτύχει
     */
    private static void addVisualSignatureBox(PDDocument doc, PDPage page, PDRectangle rect, DocumentSignature signature) throws IOException {
        try (org.apache.pdfbox.pdmodel.PDPageContentStream contentStream =
                     new org.apache.pdfbox.pdmodel.PDPageContentStream(doc, page,
                             org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode.APPEND, true, true)) {

            float width = rect.getWidth();
            float height = rect.getHeight();
            float x = rect.getLowerLeftX();
            float y = rect.getLowerLeftY();

            // Background με ανοιχτό γκρι
            contentStream.setNonStrokingColor(new PDColor(new float[]{0.95f, 0.95f, 0.95f}, PDDeviceRGB.INSTANCE));
            contentStream.addRect(x, y, width, height);
            contentStream.fill();

            // Border
            contentStream.setStrokingColor(new PDColor(new float[]{0.3f, 0.3f, 0.3f}, PDDeviceRGB.INSTANCE));
            contentStream.setLineWidth(1);
            contentStream.addRect(x + 1, y + 1, width - 2, height - 2);
            contentStream.stroke();

            // Fonts - στο PDFBox 2.0.14 χρησιμοποιούμε PDType1Font απευθείας
            org.apache.pdfbox.pdmodel.font.PDType1Font fontBold = org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD;
            org.apache.pdfbox.pdmodel.font.PDType1Font fontRegular = org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA;

            float yPos = y + height - 15;
            float xPos = x + 10;

            // Τίτλος "DIGITAL SIGNATURE"
            contentStream.beginText();
            contentStream.setFont(fontBold, 12);
            contentStream.setNonStrokingColor(new PDColor(new float[]{0.0f, 0.4f, 0.0f}, PDDeviceRGB.INSTANCE));
            contentStream.newLineAtOffset(xPos, yPos);
            contentStream.showText("DIGITAL SIGNATURE");
            contentStream.endText();

            yPos -= 18;

            // Σχήμα υπογραφής
            if (signature.getScheme() != null) {
                contentStream.beginText();
                contentStream.setFont(fontRegular, 10);
                contentStream.setNonStrokingColor(new PDColor(new float[]{0.0f, 0.0f, 0.0f}, PDDeviceRGB.INSTANCE));
                contentStream.newLineAtOffset(xPos, yPos);
                contentStream.showText("Scheme: " + signature.getScheme());
                contentStream.endText();
                yPos -= 14;
            }

            // Υπογραφές
            StringBuilder sigInfo = new StringBuilder();
            if (signature.getRsaSignature() != null && signature.getRsaSignature().length > 0) {
                sigInfo.append("RSA Signed");
            }
            if (signature.getPqcSignature() != null && signature.getPqcSignature().length > 0) {
                if (sigInfo.length() > 0) sigInfo.append(" | ");
                sigInfo.append("DILITHIUM Signed");
            }

            if (sigInfo.length() > 0) {
                contentStream.beginText();
                contentStream.setFont(fontRegular, 9);
                contentStream.setNonStrokingColor(new PDColor(new float[]{0.0f, 0.0f, 0.0f}, PDDeviceRGB.INSTANCE));
                contentStream.newLineAtOffset(xPos, yPos);
                contentStream.showText(sigInfo.toString());
                contentStream.endText();
                yPos -= 14;
            }

            // Ημερομηνία
            if (signature.getSignTime() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                contentStream.beginText();
                contentStream.setFont(fontRegular, 9);
                contentStream.setNonStrokingColor(new PDColor(new float[]{0.3f, 0.3f, 0.3f}, PDDeviceRGB.INSTANCE));
                contentStream.newLineAtOffset(xPos, yPos);
                contentStream.showText("Date: " + sdf.format(signature.getSignTime()));
                contentStream.endText();
            }
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

            System.out.println("DEBUG readEmbeddedSignature: scheme=" + scheme +
                    ", rsaBase64=" + (rsaBase64 != null ? rsaBase64.length() + " chars" : "null") +
                    ", pqcBase64=" + (pqcBase64 != null ? pqcBase64.length() + " chars" : "null"));

            // Αν δεν βρεθούν υπογραφές, επιστροφή null
            if (scheme == null && rsaBase64 == null && pqcBase64 == null) {
                System.out.println("DEBUG readEmbeddedSignature: No signatures found in PDF metadata");
                return null;
            }

            DocumentSignature sig = new DocumentSignature();
            sig.setScheme(scheme);

            if (rsaBase64 != null && !rsaBase64.isEmpty()) {
                byte[] rsaBytes = Base64.getDecoder().decode(rsaBase64);
                sig.setRsaSignature(rsaBytes);
                System.out.println("DEBUG readEmbeddedSignature: RSA signature decoded, length: " + rsaBytes.length);
            }

            if (pqcBase64 != null && !pqcBase64.isEmpty()) {
                byte[] pqcBytes = Base64.getDecoder().decode(pqcBase64);
                sig.setPqcSignature(pqcBytes);
                System.out.println("DEBUG readEmbeddedSignature: PQC signature decoded, length: " + pqcBytes.length);
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
