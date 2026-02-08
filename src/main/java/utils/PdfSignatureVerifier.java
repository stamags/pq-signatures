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
     *
     * @param pdfPath Διαδρομή προς το PDF
     * @return List με ByteRange arrays για κάθε signature, ή empty list αν δεν βρεθούν
     */
    public static List<int[]> getSignatureByteRanges(String pdfPath) throws IOException {
        List<int[]> byteRanges = new ArrayList<>();

        try (PDDocument doc = PDDocument.load(new File(pdfPath))) {
            PDAcroForm acroForm = doc.getDocumentCatalog().getAcroForm();
            if (acroForm == null) {
                return byteRanges;
            }

            // ΚΡΙΣΙΜΟ: Εύρεση signature fields που έχουν πραγματικό Contents (όχι placeholder)
            // Πρέπει να έχουν ByteRange ΚΑΙ Contents μη-κενό
            for (var field : acroForm.getFields()) {
                if (field instanceof PDSignatureField) {
                    PDSignatureField sigField = (PDSignatureField) field;
                    PDSignature pdSignature = sigField.getSignature();

                    if (pdSignature != null) {
                        // Έλεγχος αν έχει ByteRange
                        int[] byteRange = pdSignature.getByteRange();
                        if (byteRange != null && byteRange.length == 4) {
                            // ΚΡΙΣΙΜΟ: Έλεγχος αν έχει πραγματικό Contents (όχι placeholder)
                            // Το Contents είναι COSString ή COSStream στο COS object
                            org.apache.pdfbox.cos.COSBase contentsBase = pdSignature.getCOSObject().getDictionaryObject(org.apache.pdfbox.cos.COSName.CONTENTS);
                            if (contentsBase != null) {
                                // Έχει Contents, προσθέτουμε το ByteRange
                                byteRanges.add(byteRange);

                                // Debug: Ελέγχος τύπου Contents
                                if (contentsBase instanceof org.apache.pdfbox.cos.COSString) {
                                    org.apache.pdfbox.cos.COSString contentsStr = (org.apache.pdfbox.cos.COSString) contentsBase;
                                    System.out.println("DEBUG getSignatureByteRanges: Found signed field with Contents (COSString) length: " + contentsStr.getBytes().length);
                                } else if (contentsBase instanceof org.apache.pdfbox.cos.COSStream) {
                                    System.out.println("DEBUG getSignatureByteRanges: Found signed field with Contents (COSStream)");
                                } else {
                                    System.out.println("DEBUG getSignatureByteRanges: Found signed field with Contents (type: " + contentsBase.getClass().getSimpleName() + ")");
                                }
                            } else {
                                System.out.println("DEBUG getSignatureByteRanges: Skipping field with empty Contents (placeholder)");
                            }
                        }
                    }
                }
            }
        }

        return byteRanges;
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
