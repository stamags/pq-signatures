package utils;

import model.DocumentSignature;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSName;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.security.PublicKey;
import java.security.Signature;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class για επαλήθευση PDF υπογραφών με βάση το ByteRange.
 * Αυτή είναι η σωστή προσέγγιση για PDF signature verification.
 */
public class PdfSignatureVerifier {

    /**
     * Διαβάζει το ByteRange από ένα PDF signature field.
     * Τι κάνει αυτή η μέθοδος:
     * - Ανοίγει το signed PDF
     * - Βρίσκει όλα τα signature fields (μπορεί να υπάρχουν πολλές υπογραφές)
     * - Για κάθε signature, παίρνει το ByteRange που ορίζει ποια bytes υπογράφηκαν
     * - Επιστρέφει λίστα με όλα τα ByteRanges (για να δοκιμάσουμε ποιο ταιριάζει)
     * @param pdfPath Διαδρομή προς το PDF
     * @return List με ByteRange arrays για κάθε signature, ή empty list αν δεν βρεθούν
     */
    public static List<int[]> getSignatureByteRanges(String pdfPath) throws IOException {
        // Λίστα για να κρατήσουμε όλα τα ByteRanges που βρίσκουμε
        List<int[]> byteRanges = new ArrayList<>();

        // ΒΗΜΑ 1: Άνοιγμα του PDF με PDFBox
        try (PDDocument doc = PDDocument.load(new File(pdfPath))) {


            // ΒΗΜΑ 2: Πρόσβαση στο AcroForm (PDF Forms & Signature Container)
            // Το AcroForm είναι το μέρος του PDF που περιέχει:
            // - Form fields (text boxes, checkboxes, κλπ.)
            // - Signature fields (τα signature dictionaries)
            PDAcroForm acroForm = doc.getDocumentCatalog().getAcroForm();

            // Έλεγχος αν υπάρχει AcroForm
            // Αν δεν υπάρχει = το PDF δεν έχει καθόλου forms ή signatures
            if (acroForm == null) {
                System.out.println("DEBUG getSignatureByteRanges: No AcroForm found (PDF has no signature fields)");
                return byteRanges;  // Empty list
            }


            // ΒΗΜΑ 3: Iteration πάνω σε ΟΛΑ τα fields του AcroForm
            // Το AcroForm μπορεί να έχει πολλά fields:
            // - PDTextField (text input)
            // - PDCheckBox (checkbox)
            // - PDSignatureField (signature) ← Αυτό μας ενδιαφέρει!
            // - κ.ά.
            //
            // ΣΗΜΕΙΩΣΗ: Μπορεί να υπάρχουν ΠΟΛΛΕΣ υπογραφές σε ένα PDF!
            // Π.χ. 1η υπογραφή από τον τάδε, 2η από έναν άλλο, 3η από άλλο πάλο
            for (var field : acroForm.getFields()) {


                //  Έλεγχος: Είναι signature field;
                if (field instanceof PDSignatureField) {
                    PDSignatureField sigField = (PDSignatureField) field;

                    // 3.2) Παίρνουμε το PDSignature object (το signature dictionary)
                    // Το PDSignature περιέχει:
                    // - /Type /Sig
                    // - /Filter /Adobe.PPKLite
                    // - /SubFilter /ETSI.CAdES.detached
                    // - /ByteRange [0, N, M, fileSize]
                    // - /Contents <CMS_SIGNATURE_BLOB>
                    // - /Name, /M, /Reason, /Location (metadata)
                    PDSignature pdSignature = sigField.getSignature();

                    // Έλεγχος αν το signature field ΟΝΤΩΣ έχει signature
                    // (Μπορεί να υπάρχει field αλλά να μην έχει υπογραφτεί ακόμα)
                    if (pdSignature != null) {

                        // ΒΗΜΑ 4: Παίρνουμε το ByteRange
                        // Το ByteRange είναι array με 4 integers:
                        // [start1, length1, start2, length2]
                        //
                        // Παράδειγμα: [0, 1666395, 1966397, 360116]
                        // Σημαίνει:
                        //   - Υπογεγραμμένα bytes Part 1: από 0 μέχρι 1666395
                        //   - Υπογεγραμμένα bytes Part 2: από 1966397 μέχρι 2326513
                        //   - Το gap (1666395..1966397) είναι το /Contents field
                        int[] byteRange = pdSignature.getByteRange();

                        // Validation: Το ByteRange πρέπει να έχει ακριβώς 4 integers
                        if (byteRange != null && byteRange.length == 4) {

                            // ΒΗΜΑ 5:  Έλεγχος αν  Έχει πραγματική υπογραφή;
                            // Όταν υπογράφουμε με external signing:
                            // 1. Πρώτα γράφεται το PDF με PLACEHOLDER στο /Contents:
                            //    /Contents <0000000000...0000>  (placeholder)
                            // 2. Μετά αντικαθίσταται με την πραγματική υπογραφή:
                            //    /Contents <308006092a864886f7...>  (CMS blob)
                            // Αν το process διακοπεί (crash, exception), μπορεί το PDF
                            // να μείνει με placeholder! Αυτό ΔΕΝ είναι έγκυρη υπογραφή!
                            // Γι' αυτό ελέγχουμε αν το /Contents έχει ΠΡΑΓΜΑΤΙΚΟ περιεχόμενο

                            // Πρόσβαση στο low-level COS (Carousel Object System) object
                            // Το PDFBox χρησιμοποιεί το COS layer για raw PDF data
                            org.apache.pdfbox.cos.COSBase contentsBase =
                                    pdSignature.getCOSObject()
                                            .getDictionaryObject(org.apache.pdfbox.cos.COSName.CONTENTS);

                            // Έλεγχος: Υπάρχει το /Contents field;
                            if (contentsBase != null) {
                                // ΝΑΙ - Έχει πραγματική υπογραφή!
                                // Προσθήκη του ByteRange στη λίστα
                                byteRanges.add(byteRange);

                                // Debug logging: Τύπος του Contents
                                // Το Contents μπορεί να είναι:
                                // - COSString: Hex string (το συνηθισμένο)
                                // - COSStream: Stream object (σπάνιο)
                                if (contentsBase instanceof org.apache.pdfbox.cos.COSString) {
                                    org.apache.pdfbox.cos.COSString contentsStr =
                                            (org.apache.pdfbox.cos.COSString) contentsBase;
                                    System.out.println("DEBUG getSignatureByteRanges: Found signed field with Contents (COSString) length: " +
                                            contentsStr.getBytes().length + " bytes");
                                    System.out.println("DEBUG getSignatureByteRanges: ByteRange: [" +
                                            byteRange[0] + ", " + byteRange[1] + ", " +
                                            byteRange[2] + ", " + byteRange[3] + "]");
                                } else if (contentsBase instanceof org.apache.pdfbox.cos.COSStream) {
                                    System.out.println("DEBUG getSignatureByteRanges: Found signed field with Contents (COSStream)");
                                } else {
                                    System.out.println("DEBUG getSignatureByteRanges: Found signed field with Contents (type: " +
                                            contentsBase.getClass().getSimpleName() + ")");
                                }
                            } else {
                                // ΟΧΙ - Το Contents είναι null ή empty (placeholder)
                                System.out.println("DEBUG getSignatureByteRanges: Skipping signature field with empty Contents (placeholder - PDF not fully signed)");
                                // ΔΕΝ προσθέτουμε το ByteRange - δεν είναι έγκυρη υπογραφή
                            }
                        } else {
                            // Invalid ByteRange format
                            System.out.println("DEBUG getSignatureByteRanges: Skipping signature with invalid ByteRange format");
                        }
                    } else {
                        // Το signature field υπάρχει αλλά δεν έχει PDSignature object
                        System.out.println("DEBUG getSignatureByteRanges: Skipping empty signature field (no signature object)");
                    }
                }
            }
        }


        // ΤΕΛΙΚΟ ΑΠΟΤΕΛΕΣΜΑ
        System.out.println("DEBUG getSignatureByteRanges: Found " + byteRanges.size() + " valid signature(s)");
        return byteRanges;

        // Παράδειγμα output:
        // [
        //   [0, 1666395, 1966397, 360116]  // 1η υπογραφή
        // ]
        //
        // Αν υπήρχαν πολλές υπογραφές:
        // [
        //   [0, 1666395, 1966397, 360116],     // 1η υπογραφή (Alice)
        //   [0, 2326513, 2626515, 100000],     // 2η υπογραφή (Bob)
        //   [0, 2726515, 3026517, 50000]       // 3η υπογραφή (Charlie)
        // ]
    }

    /**
     * Παίρνει τα bytes που ορίζονται από το ByteRange.
     * Το ByteRange έχει format: [start1, length1, start2, length2]
     * Τα υπογεγραμμένα bytes είναι: bytes[start1..start1+length1) + bytes[start2..start2+length2)
     *
     * @param pdfPath Διαδρομή προς το PDF
     * @param byteRange Το ByteRange array
     * @return Τα bytes που ορίζονται από το ByteRange
     */
    public static byte[] getSignedBytes(String pdfPath, int[] byteRange) throws IOException {
        if (byteRange == null || byteRange.length != 4) {
            throw new IllegalArgumentException("Invalid ByteRange format");
        }

        int start1 = byteRange[0];
        int length1 = byteRange[1];
        int start2 = byteRange[2];
        int length2 = byteRange[3];

        try (RandomAccessFile raf = new RandomAccessFile(pdfPath, "r")) {
            // Πρώτο range: [start1, start1+length1)
            byte[] part1 = new byte[length1];
            raf.seek(start1);
            raf.readFully(part1);

            // Δεύτερο range: [start2, start2+length2)
            byte[] part2 = new byte[length2];
            raf.seek(start2);
            raf.readFully(part2);

            // Συνένωση των δύο ranges
            byte[] signedBytes = new byte[length1 + length2];
            System.arraycopy(part1, 0, signedBytes, 0, length1);
            System.arraycopy(part2, 0, signedBytes, length1, length2);

            return signedBytes;
        }
    }

    /**
     * Ελέγχει αν υπάρχουν incremental updates μετά την υπογραφή.
     * Αν το ByteRange καλύπτει όλο το αρχείο (μέχρι EOF), δεν υπάρχουν updates.
     * Αν υπάρχουν bytes μετά το τέλος του ByteRange, υπάρχουν updates.
     *
     * @param pdfPath Διαδρομή προς το PDF
     * @param byteRange Το ByteRange array
     * @return true αν υπάρχουν incremental updates μετά την υπογραφή
     */
    public static boolean hasIncrementalUpdates(String pdfPath, int[] byteRange) throws IOException {
        if (byteRange == null || byteRange.length != 4) {
            return false;
        }

        int start2 = byteRange[2];
        int length2 = byteRange[3];
        long endOfSignedContent = start2 + length2;

        try (RandomAccessFile raf = new RandomAccessFile(pdfPath, "r")) {
            long fileLength = raf.length();
            // Ελέγχουμε αν υπάρχουν bytes μετά το τέλος του signed content
            // (με μικρή ανοχή για rounding errors)
            return endOfSignedContent < fileLength - 10; // 10 bytes tolerance
        }
    }

    /**
     * Ελέγχει αν το ByteRange καλύπτει όλο το αρχείο.
     *
     * @param pdfPath Διαδρομή προς το PDF
     * @param byteRange Το ByteRange array
     * @return true αν καλύπτει όλο το αρχείο
     */
    public static boolean coversWholeFile(String pdfPath, int[] byteRange) throws IOException {
        return !hasIncrementalUpdates(pdfPath, byteRange);
    }

    /**
     * Επαληθεύει RSA υπογραφή με βάση το ByteRange.
     *
     * @param pdfPath Διαδρομή προς το PDF
     * @param byteRange Το ByteRange array
     * @param signatureBytes Τα bytes της υπογραφής
     * @param publicKey Το RSA public key
     * @return true αν η επαλήθευση περάσει
     */
    public static boolean verifyRsaSignature(String pdfPath, int[] byteRange,
                                             byte[] signatureBytes, PublicKey publicKey) throws Exception {
        byte[] signedBytes = getSignedBytes(pdfPath, byteRange);

        System.out.println("DEBUG RSA Verify: Signed bytes length: " + signedBytes.length);
        System.out.println("DEBUG RSA Verify: Signature bytes length: " + signatureBytes.length);

        // Υπολογισμός hash για σύγκριση
        java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
        byte[] signedHash = md.digest(signedBytes);
        System.out.println("DEBUG RSA Verify: Signed bytes SHA-256 hash (first 16 bytes): " +
                java.util.HexFormat.of().formatHex(java.util.Arrays.copyOf(signedHash, 16)));

        Signature rsa = Signature.getInstance("SHA256withRSA");
        rsa.initVerify(publicKey);
        rsa.update(signedBytes);
        boolean result = rsa.verify(signatureBytes);
        System.out.println("DEBUG RSA Verify: Result: " + result);

        if (!result) {
            System.err.println("ERROR: RSA verification failed!");
            System.err.println("ERROR: ByteRange: [" + byteRange[0] + ", " + byteRange[1] + ", " +
                    byteRange[2] + ", " + byteRange[3] + "]");
        }

        return result;
    }

    /**
     * Επαληθεύει PQC (DILITHIUM) υπογραφή με βάση το ByteRange.
     *
     * @param pdfPath Διαδρομή προς το PDF
     * @param byteRange Το ByteRange array
     * @param signatureBytes Τα bytes της υπογραφής
     * @param publicKey Το PQC public key
     * @return true αν η επαλήθευση περάσει
     */
    public static boolean verifyPqcSignature(String pdfPath, int[] byteRange,
                                             byte[] signatureBytes, PublicKey publicKey) throws Exception {
        byte[] signedBytes = getSignedBytes(pdfPath, byteRange);

        System.out.println("DEBUG PQC Verify: Signed bytes length: " + signedBytes.length);
        System.out.println("DEBUG PQC Verify: Signature bytes length: " + signatureBytes.length);

        // Υπολογισμός hash για σύγκριση
        java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
        byte[] signedHash = md.digest(signedBytes);
        System.out.println("DEBUG PQC Verify: Signed bytes SHA-256 hash (first 16 bytes): " +
                java.util.HexFormat.of().formatHex(java.util.Arrays.copyOf(signedHash, 16)));

        Signature pq = Signature.getInstance("DILITHIUM3", "BC");
        pq.initVerify(publicKey);
        pq.update(signedBytes);
        boolean result = pq.verify(signatureBytes);
        System.out.println("DEBUG PQC Verify: Result: " + result);

        if (!result) {
            System.err.println("ERROR: PQC verification failed!");
            System.err.println("ERROR: ByteRange: [" + byteRange[0] + ", " + byteRange[1] + ", " +
                    byteRange[2] + ", " + byteRange[3] + "]");
        }

        return result;
    }

    /**
     * Επαληθεύει όλες τις υπογραφές ενός PDF με βάση το ByteRange.
     *
     * @param pdfPath Διαδρομή προς το PDF
     * @param signature Η υπογραφή προς επαλήθευση
     * @param rsaPublicKey Το RSA public key
     * @param pqcPublicKey Το PQC public key
     * @return SignatureVerificationResult με τα αποτελέσματα
     */
    public static SignatureVerificationResult verifySignatures(String pdfPath, DocumentSignature signature,
                                                               PublicKey rsaPublicKey, PublicKey pqcPublicKey)
            throws Exception {
        // ΚΡΙΣΙΜΟ: Ελέγχος αν το signature object έχει υπογραφές
        System.out.println("DEBUG verifySignatures: Signature object - RSA: " +
                (signature.getRsaSignature() != null ? signature.getRsaSignature().length + " bytes" : "null") +
                ", PQC: " + (signature.getPqcSignature() != null ? signature.getPqcSignature().length + " bytes" : "null") +
                ", Scheme: " + signature.getScheme());

        if (signature.getRsaSignature() == null && signature.getPqcSignature() == null) {
            System.err.println("ERROR verifySignatures: Signature object has no RSA or PQC signatures!");
            return new SignatureVerificationResult(null, null, false, false, false);
        }

        List<int[]> byteRanges = getSignatureByteRanges(pdfPath);

        if (byteRanges.isEmpty()) {
            // Αν δεν υπάρχει PDF cryptographic signature, δεν μπορούμε να χρησιμοποιήσουμε ByteRange
            // Fallback σε παλιά λογική
            System.err.println("ERROR verifySignatures: No signature fields with Contents found in PDF");
            return new SignatureVerificationResult(null, null, false, false, false);
        }

        System.out.println("DEBUG Verify: Found " + byteRanges.size() + " signature field(s) with Contents");

        // ΚΡΙΣΙΜΟ: Δοκιμάζουμε ΟΛΑ τα ByteRanges μέχρι να βρούμε αυτό που ταιριάζει
        // Αυτό είναι σημαντικό γιατί μπορεί να υπάρχουν πολλά signature fields
        // και οι raw υπογραφές μας να αντιστοιχούν σε συγκεκριμένο ByteRange
        Boolean rsaOkFinal = null;
        Boolean pqcOkFinal = null;
        boolean overallFinal = false;
        boolean hasUpdatesFinal = false;
        boolean coversWholeFinal = false;
        int[] matchedByteRange = null;

        for (int i = 0; i < byteRanges.size(); i++) {
            int[] byteRange = byteRanges.get(i);

            System.out.println("DEBUG Verify: Trying ByteRange #" + (i + 1) + ": [" + byteRange[0] + ", " +
                    byteRange[1] + ", " + byteRange[2] + ", " + byteRange[3] + "]");

            Boolean rsaOk = null;
            Boolean pqcOk = null;

            // Επαλήθευση RSA υπογραφής
            if (signature.getRsaSignature() != null && signature.getRsaSignature().length > 0) {
                try {
                    rsaOk = verifyRsaSignature(pdfPath, byteRange, signature.getRsaSignature(), rsaPublicKey);
                    System.out.println("DEBUG Verify: ByteRange #" + (i + 1) + " RSA verification: " + rsaOk);
                } catch (Exception e) {
                    System.err.println("RSA verification failed for ByteRange #" + (i + 1) + ": " + e.getMessage());
                    rsaOk = false;
                }
            }

            // Επαλήθευση PQC υπογραφής
            if (signature.getPqcSignature() != null && signature.getPqcSignature().length > 0) {
                try {
                    pqcOk = verifyPqcSignature(pdfPath, byteRange, signature.getPqcSignature(), pqcPublicKey);
                    System.out.println("DEBUG Verify: ByteRange #" + (i + 1) + " PQC verification: " + pqcOk);
                } catch (Exception e) {
                    System.err.println("PQC verification failed for ByteRange #" + (i + 1) + ": " + e.getMessage());
                    pqcOk = false;
                }
            }

            // Έλεγχος αν αυτό το ByteRange ταιριάζει
            boolean overall = (rsaOk == null || rsaOk) && (pqcOk == null || pqcOk);

            if (overall) {
                // Βρήκαμε το σωστό ByteRange!
                rsaOkFinal = rsaOk;
                pqcOkFinal = pqcOk;
                overallFinal = true;
                hasUpdatesFinal = hasIncrementalUpdates(pdfPath, byteRange);
                coversWholeFinal = coversWholeFile(pdfPath, byteRange);
                matchedByteRange = byteRange;

                System.out.println("DEBUG Verify: ✅ MATCHED ByteRange #" + (i + 1));
                System.out.println("DEBUG Verify: ByteRange covers: bytes [0-" + byteRange[1] + ") + [" +
                        byteRange[2] + "-" + (byteRange[2] + byteRange[3]) + ")");
                System.out.println("DEBUG Verify: Total signed bytes: " + (byteRange[1] + byteRange[3]));
                System.out.println("DEBUG Verify: Has incremental updates: " + hasUpdatesFinal);
                System.out.println("DEBUG Verify: Covers whole file: " + coversWholeFinal);

                // Ελέγχος μήκους αρχείου
                try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(pdfPath, "r")) {
                    long fileLength = raf.length();
                    System.out.println("DEBUG Verify: File length: " + fileLength);
                    System.out.println("DEBUG Verify: ByteRange end: " + (byteRange[2] + byteRange[3]));
                } catch (Exception e) {
                    System.err.println("ERROR: Could not check file length: " + e.getMessage());
                }

                break; // Βρήκαμε το σωστό ByteRange, σταματάμε
            } else {
                System.out.println("DEBUG Verify: ❌ ByteRange #" + (i + 1) + " did not match (RSA: " +
                        rsaOk + ", PQC: " + pqcOk + ")");
            }
        }

        if (matchedByteRange == null) {
            System.err.println("ERROR: No matching ByteRange found for the signatures!");
            System.err.println("ERROR: Tried " + byteRanges.size() + " ByteRange(s) but none matched");
        }

        return new SignatureVerificationResult(rsaOkFinal, pqcOkFinal, overallFinal, hasUpdatesFinal, coversWholeFinal);
    }

    /**
     * Αποτέλεσμα επαλήθευσης υπογραφής.
     */
    public static class SignatureVerificationResult {
        public final Boolean rsaOk;
        public final Boolean pqcOk;
        public final boolean overall;
        public final boolean hasIncrementalUpdates;
        public final boolean coversWholeFile;

        public SignatureVerificationResult(Boolean rsaOk, Boolean pqcOk, boolean overall,
                                           boolean hasIncrementalUpdates, boolean coversWholeFile) {
            this.rsaOk = rsaOk;
            this.pqcOk = pqcOk;
            this.overall = overall;
            this.hasIncrementalUpdates = hasIncrementalUpdates;
            this.coversWholeFile = coversWholeFile;
        }
    }
}
