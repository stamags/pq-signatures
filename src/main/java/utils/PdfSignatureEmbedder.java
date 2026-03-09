package utils;

import model.DocumentSignature;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.ExternalSigningSupport;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import signatures.KeyLoader;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Arrays;
import java.util.Calendar;

/**
 * Utility class για ενσωμάτωση ψηφιακών υπογραφών σε PDF αρχεία.
 *
 * Υποστηρίζει:
 * - RSA υπογραφές (κλασική κρυπτογραφία)
 * - Dilithium υπογραφές (post-quantum κρυπτογραφία)
 * - Hybrid υπογραφές (RSA + Dilithium)
 *
 * Χρησιμοποιεί external signing με PDFBox για πλήρη έλεγχο του signing process.
 */
public class PdfSignatureEmbedder {

    private PdfSignatureEmbedder() {}

    public static void embedSignature(Long documentId, DocumentSignature signature, String username, char[] keystorePassword) throws Exception {
        // Βρίσκουμε τη διαδρομή του PDF στο filesystem
        Path filePath = FileStorageService.getFilePath(documentId);

        // Έλεγχος ύπαρξης αρχείου
        if (!Files.exists(filePath)) {
            throw new Exception("PDF file not found for document ID: " + documentId);
        }

        // Κλήση της κύριας μεθόδου υπογραφής
        addPdfCryptographicSignature(filePath.toString(), signature, username, keystorePassword);
    }

    /**
     * Η κύρια μέθοδος που πραγματοποιεί την κρυπτογραφική υπογραφή του PDF.
     *
     * EXTERNAL SIGNING FLOW (βήμα-βήμα):
     * 1. ΠΡΟΕΤΟΙΜΑΣΙΑ PDF:
     *    - Φόρτωση του PDF με PDFBox
     *    - Δημιουργία PDSignature (signature dictionary με metadata)
     *    - Προσθήκη visual signature (πράσινο κουτί)
     * 2. EXTERNAL SIGNING SETUP:
     *    - Καλούμε saveIncrementalForExternalSigning()
     *    - Το PDFBox γράφει το PDF με placeholder στο /Contents
     *    - Υπολογίζει το ByteRange: [start1, length1, start2, length2]
     *    - Μας δίνει τα ByteRange bytes (αυτά που πρέπει να υπογράψουμε)
     * 3. ΥΠΟΓΡΑΦΗ (3 φορές πάνω στα ΙΔΙΑ bytes!):
     *    A) Raw RSA signature → αποθήκευση στο signature object (για DB)
     *    B) Raw PQC signature → αποθήκευση στο signature object (για DB)
     *    C) CMS/PKCS#7 (RSA) → για το /Contents του PDF (Adobe Reader compatibility)
     * 4. ΕΝΣΩΜΑΤΩΣΗ:
     *    - Το CMS blob μπαίνει στο /Contents field του PDF
     *    - Το temp signed PDF αντικαθιστά το original
     * ΑΠΟΤΕΛΕΣΜΑ:
     * - Signed PDF στο filesystem (με CMS στο /Contents)
     * - Raw signatures στο signature object (για αποθήκευση στη DB)
     *
     * @param pdfPath          Η διαδρομή του PDF αρχείου
     * @param signature        Το DocumentSignature object (θα γεμίσει με rsaSignature, pqcSignature)
     * @param username         Το username του υπογράφοντα
     * @param keystorePassword Ο κωδικός του keystore
     * @throws Exception       Αν αποτύχει η φόρτωση του PDF ή η υπογραφή
     */
    private static void addPdfCryptographicSignature(String pdfPath, DocumentSignature signature, String username, char[] keystorePassword) throws Exception {
        Path original = Paths.get(pdfPath);
        // Δημιουργούμε temp file για το signed PDF (δεν αλλάζουμε το original αμέσως)
        Path temp = Files.createTempFile("pdf_signed_", ".pdf");

        try (PDDocument doc = PDDocument.load(original.toFile())) {

            // ΒΗΜΑ 1: Δημιουργία Signature Dictionary (Metadata Container)
            // Το PDSignature  είναι το metadata container!
            // Περιέχει: ποιος, πότε, πού, πώς (Filter, SubFilter, ByteRange, κλπ.)
            PDSignature pdSignature = new PDSignature();


            // Ορίζουμε την ημερομηνία Υπογραφής
            Calendar cal = Calendar.getInstance();
            if (signature.getSignTime() != null) {
                cal.setTime(signature.getSignTime());
            }
            pdSignature.setSignDate(cal);

            // Ορίζουμε τον  handler που θα επεξεργαστεί την υπογραφή
            // /Filter /Adobe.PPKLite έχει Universal compatibility (όλα τα PDF readers το υποστηρίζουν)
            pdSignature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);


            // SubFilter: Το format της υπογραφής (πώς είναι δομημένη)
            // /SubFilter /ETSI.CAdES.detached
            // Δηλαδή η υπογραφή είναι CAdES detached signature
            // PAdES-compliant (European standard)
            // eIDAS compatible
            pdSignature.setSubFilter(PDSignature.SUBFILTER_ETSI_CADES_DETACHED);

            // Metadata: Ποιος, Γιατί, Πού
            // /Name field - εμφανίζεται στο Adobe Reader ως "Signed by: ..."
            String scheme = normalizeScheme(signature.getScheme());
            String nameRaw = (username != null && !username.isEmpty() ? username : "PQ-Signatures System")
                    + (scheme.isEmpty() ? "" : " (" + scheme + ")");
            pdSignature.setName(PdfVisualSignatureUtil.toLatinForPdf(nameRaw));

            // /Reason field - γιατί υπογράφηκε
            pdSignature.setReason("Digital Signature");

            // /Location field - πού υπογράφηκε
            pdSignature.setLocation("Greece");

            // ΒΗΜΑ 2:  Signature Options + Visual Signature (Οπτική Εμφάνιση)
            SignatureOptions options = new SignatureOptions();

            // Preferred size για το /Contents field (150KB είναι αρκετό για CMS + cert)
            options.setPreferredSignatureSize(150000);

            // Η υπογραφή θα εμφανιστεί στην πρώτη σελίδα (page 0)
            options.setPage(0);

            // Υπολογισμός θέσης για το οπτικό κουτί υπογραφής
            // Παίρνουμε το μέγεθος της σελίδας
            PDRectangle pageSize = doc.getPage(0).getMediaBox();

            // Ορισμός διαστάσεων και θέσης (σε PDF points, 1 point = 1/72 inch)
            float margin = 20f;   // 20 points απόσταση από άκρα
            float width = 250f;   // Πλάτος κουτιού
            float height = 80f;   // Ύψος κουτιού

            // Θέση: Πάνω-αριστερά με margin
            // Σημείωση: Το PDF coordinate system έχει (0,0) κάτω-αριστερά
            // Γι' αυτό για να βάλουμε το κουτί "πάνω", αφαιρούμε από το pageHeight
            float x = margin;
            float y = pageSize.getHeight() - height - margin;

            // Δημιουργία rectangle για το visual signature
            PDRectangle rect = new PDRectangle(x, y, width, height);

            // 2.2) Δημιουργία visual signature template (πράσινο κουτί με κείμενο)
            // Το PdfVisualSignatureUtil δημιουργεί ένα PDFormXObject με:
            // - Πράσινο background
            // - Κείμενο: "Digitally Signed by [username]"
            // - Timestamp
            // - Scheme (RSA/DILITHIUM/HYBRID)
            options.setVisualSignature(
                    PdfVisualSignatureUtil.createVisualSignatureTemplate(doc, 0, rect, signature)
            );

            // ΒΗΜΑ 3: Προετοιμασία για External Signing
            // Περνάμε null ως SignatureInterface διότι είναι σαν να του λέμε ότι θα υπογράψω εγώ εξωτερικά
            doc.addSignature(pdSignature, (SignatureInterface) null, options);

            // Τι κάνει το addSignature():
            // - Προσθέτει το signature dictionary στο PDF
            // - Δημιουργεί signature field στο AcroForm
            // - Προετοιμάζει placeholder για το /Contents (με 0x00 bytes)


            // ΒΗΜΑ 4: External Signing Session
            try (FileOutputStream fos = new FileOutputStream(temp.toFile())) {
                // Incremental Save για External Signing
                // Αυτή η μέθοδος:
                // 1. Γράφει το PDF στο temp file (με placeholder στο /Contents)
                // 2. Υπολογίζει το ByteRange: [start1, length1, start2, length2]
                // 3. Επιστρέφει ExternalSigningSupport object που μας δίνει:
                //    - Τα ByteRange bytes (μέσω getContent())
                //    - Δυνατότητα να βάλουμε την υπογραφή αργότερα (μέσω setSignature())
                ExternalSigningSupport externalSigning = doc.saveIncrementalForExternalSigning(fos);

                // Λήψη ByteRange Bytes (ΤΑ BYTES ΠΟΥ ΘΑ ΥΠΟΓΡΑΨΟΥΜΕ!)
                // Αυτά τα bytes είναι ΟΛΟ το PDF ΕΚΤΟΣ του /Contents field
                // ByteRange = [0, N, M, fileSize]
                //   Part 1: bytes από 0 μέχρι N (πριν το /Contents)
                //   Part 2: bytes από M μέχρι fileSize (μετά το /Contents)
                // Αυτά είναι τα ακριβή bytes που ορίζει το PDF standard για signing
                byte[] bytesToSign = externalSigning.getContent().readAllBytes();

                // 4.3) Debug: SHA-256 των ByteRange bytes (προαιρετικό)
                // Υπολογίζουμε το hash για debugging/verification
                // (Δεν το χρησιμοποιούμε για υπογραφή - υπογράφουμε τα raw bytes)
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] hash = md.digest(bytesToSign);
                System.out.println("DEBUG ByteRange bytes len=" + bytesToSign.length +
                        ", SHA-256(first16)=" + toHex(Arrays.copyOf(hash, 16)));


                // ΒΗΜΑ 5: Raw Signatures για Database (RSA + PQC)
                // Υπογράφουμε τα ByteRange bytes με RSA και/ή Dilithium
                // Οι raw signatures αποθηκεύονται στο signature object
                // Αυτές ΔΕΝ μπαίνουν στο PDF - μόνο στη DB για verification  Γιατί το PDF format δεν υποστηρίζει Dilithium στο /Contents
                signRawBytesForDatabase(signature, scheme, bytesToSign, username, keystorePassword);


                // ΒΗΜΑ 6: CMS/PKCS#7 Signature για PDF /Contents
                // Δημιουργούμε RSA signature σε CMS/PKCS#7 format
                // Αυτό είναι το blob που θα μπει στο /Contents του PDF
                // Περιέχει: RSA signature + X.509 certificate + PKCS#7 wrapper
                // Λόγος: Για Adobe Reader compatibility (PAdES standard)
                RsaSignatureInterface signatureInterface = new RsaSignatureInterface(username, keystorePassword);
                byte[] cmsSignature = signatureInterface.sign(new ByteArrayInputStream(bytesToSign));

                // ΒΗΜΑ 7: Ενσωμάτωση CMS στο PDF
                // Βάζουμε το CMS blob στο /Contents field
                // Το PDFBox αντικαθιστά το placeholder (0x00...) με το πραγματικό CMS
                externalSigning.setSignature(cmsSignature);

                // ΤΙ ΕΧΟΥΜΕ ΤΩΡΑ:
                // ✅ Temp file με signed PDF (CMS στο /Contents)
                // ✅ signature object με raw RSA/PQC signatures (για DB)
            }
        }


        // ΒΗΜΑ 8: Αντικατάσταση Original PDF με Signed PDF
        // Το temp signed PDF γίνεται το νέο original
        // Αυτό διατηρεί το ίδιο path στο filesystem
        Files.move(temp, original, StandardCopyOption.REPLACE_EXISTING);

        // ΤΕΛΙΚΟ ΑΠΟΤΕΛΕΣΜΑ:
        // 1. Signed PDF στο filesystem:
        //    - Με signature dictionary (metadata)
        //    - Με ByteRange field
        //    - Με CMS blob στο /Contents (RSA signature)
        //    - Με visual signature (πράσινο κουτί)
        //
        // 2. signature object (DocumentSignature):
        //    - rsaSignature: raw RSA bytes (για DB verification)
        //    - pqcSignature: raw Dilithium bytes (για DB verification, αν HYBRID)
        //    - scheme: RSA / DILITHIUM / HYBRID
        //    - signTime: timestamp
        //
        // 3. Το bean θα αποθηκεύσει το signature object στη DB
    }

    /**
     * Δημιουργεί raw signatures (RSA και/ή PQC) πάνω στα ByteRange bytes.
     * Αυτές οι υπογραφές:
     * - ΔΕΝ μπαίνουν στο PDF
     * - Αποθηκεύονται στη βάση δεδομένων (BLOB fields)
     * - Χρησιμοποιούνται για verification αργότερα
     * Γιατί raw και όχι CMS;
     * - Το CMS format (PKCS#7) υποστηρίζει μόνο κλασικούς αλγορίθμους (RSA, ECDSA)
     * - Το Dilithium (PQC) ΔΕΝ υποστηρίζεται σε CMS/PKCS#7
     * - Γι' αυτό κρατάμε τις raw signatures στη DB για να επαληθεύουμε και το PQC
     *
     * @param signatureObj     Το DocumentSignature object (θα γεμίσει με τις raw signatures)
     * @param scheme           Το scheme: "RSA", "DILITHIUM", ή "HYBRID"
     * @param bytesToSign      Τα ByteRange bytes από το PDF
     * @param username         Το username για φόρτωση κλειδιών
     * @param keystorePassword Ο κωδικός keystore για RSA private key
     * @throws Exception       Αν αποτύχει η φόρτωση κλειδιών ή η υπογραφή
     */
    private static void signRawBytesForDatabase(DocumentSignature signatureObj, String scheme, byte[] bytesToSign,
                                                String username, char[] keystorePassword) throws Exception {
        // RSA RAW SIGNATURE (για DB)
        // Αν το scheme είναι RSA ή HYBRID, υπογράφουμε με RSA
        if ("RSA".equals(scheme) || "HYBRID".equals(scheme)) {
            // Φόρτωση RSA private key του χρήστη (από keystore)
            PrivateKey rsaPriv = KeyLoader.loadRsaPrivateKeyForUser(username, keystorePassword);

            // Δημιουργία RSA signature instance (Java Crypto Architecture)
            // Αλγόριθμος: SHA256withRSA (hash με SHA-256, υπογραφή με RSA)
            Signature rsa = Signature.getInstance("SHA256withRSA");

            // Αρχικοποίηση με το private key
            rsa.initSign(rsaPriv);

            // Τάισμα των bytes προς υπογραφή
            // Αυτά είναι τα ByteRange bytes από το PDF
            rsa.update(bytesToSign);

            // Υπολογισμός της υπογραφής (raw bytes)
            // Αυτή είναι η κρυπτογραφική υπογραφή: σχεδόν 256 bytes για RSA-2048
            byte[] rsaSignatureBytes = rsa.sign();

            // Αποθήκευση στο DocumentSignature object
            // Αυτό θα πάει στη DB ως BLOB
            signatureObj.setRsaSignature(rsaSignatureBytes);

            // Debug output
            System.out.println("DEBUG Raw RSA signature len=" + signatureObj.getRsaSignature().length);
        } else {
            // Αν το scheme είναι μόνο DILITHIUM (όχι RSA), βάζουμε null
            signatureObj.setRsaSignature(null);
        }


        // PQC (DILITHIUM) RAW SIGNATURE (για DB)
        // Αν το scheme είναι DILITHIUM ή HYBRID, υπογράφουμε με Dilithium
        if ("DILITHIUM".equals(scheme) || "HYBRID".equals(scheme)) {
            // Φόρτωση Dilithium private key του χρήστη (από αρχείο)
            PrivateKey pqPriv = KeyLoader.loadPqcPrivateKeyForUser(username);

            // Δημιουργία Dilithium signature instance
            // Αλγόριθμος: DILITHIUM3 (NIST Level 3, ~AES-192 security)
            // Provider: "BC" (Bouncy Castle - η μόνη που υποστηρίζει Dilithium σε Java)
            Signature pq = Signature.getInstance("DILITHIUM3", "BC");

            // Αρχικοποίηση με το private key
            pq.initSign(pqPriv);

            // Τάισμα των ΙΔΙΩΝ bytes που υπογράψαμε με RSA
            // Αυτό εξασφαλίζει ότι και οι 2 υπογραφές είναι πάνω στο ίδιο content
            pq.update(bytesToSign);

            // Υπολογισμός της υπογραφής (raw bytes)
            // Η Dilithium3 signature είναι ~3.3 KB (μεγαλύτερη από RSA)
            byte[] pqcSignatureBytes = pq.sign();

            // Αποθήκευση στο DocumentSignature object
            // Αυτό θα πάει στη DB ως BLOB
            signatureObj.setPqcSignature(pqcSignatureBytes);

            // Debug output
            System.out.println("DEBUG Raw PQC signature len=" + signatureObj.getPqcSignature().length);
        } else {
            // Αν το scheme είναι μόνο RSA (όχι PQC), βάζουμε null
            signatureObj.setPqcSignature(null);
        }


        // ΣΗΜΕΙΩΣΗ: Hybrid Signatures
        // Όταν scheme = "HYBRID":
        // - Και οι 2 υπογραφές (RSA + PQC) υπολογίζονται
        // - Και οι 2 πάνω στα ΙΔΙΑ ByteRange bytes
        // - Αποθηκεύονται και οι 2 στη DB
        // - Στο PDF μπαίνει μόνο το RSA CMS (για Adobe Reader)
        // - Κατά την επαλήθευση, ελέγχουμε και τις 2:
        //   * rsaOk = verify(rsaSignature, bytesToSign, rsaPublicKey)
        //   * pqcOk = verify(pqcSignature, bytesToSign, pqcPublicKey)
        //   * overall = rsaOk && pqcOk
    }

    /**
     * Κανονικοποιεί το scheme string σε uppercase.
     *
     * @param s Το scheme string (μπορεί να είναι null)
     * @return Το scheme σε uppercase, ή "" αν είναι null
     */
    private static String normalizeScheme(String s) {
        return s == null ? "" : s.trim().toUpperCase();
    }

    /**
     * Μετατρέπει byte array σε hexadecimal string.
     * Χρησιμοποιείται για debug output.
     *
     * @param b Τα bytes προς μετατροπή
     * @return Hex string (π.χ. "a3f5c9e2...")
     */
    private static String toHex(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (byte v : b) sb.append(String.format("%02x", v));
        return sb.toString();
    }
}
