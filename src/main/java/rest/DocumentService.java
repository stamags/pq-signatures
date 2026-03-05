package rest;

import model.DocumentSignature;
import model.DocumentFile;
import model.Tbluser;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import signatures.KeyLoader;
import db.JPAUtil;
import utils.FileStorageService;
import utils.PdfSignatureEmbedder;
import utils.PdfSignatureVerifier;

import java.nio.file.Path;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import java.security.*;
import java.util.List;

public class DocumentService {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public DocumentService() {
        // Προεπιλεγμένος constructor - χρησιμοποιεί static dbTransactions methods
    }

//    public Long saveDocument(String filename, String contentType, byte[] pdfBytes) throws Exception {
//        if (filename == null || filename.trim().isEmpty()) {
//            throw new IllegalArgumentException("Filename cannot be null or empty");
//        }
//        if (pdfBytes == null || pdfBytes.length == 0) {
//            throw new IllegalArgumentException("PDF bytes cannot be null or empty");
//        }
//
//        // Υπολογισμός SHA-256
//        String sha256 = FileStorageService.calculateSha256(pdfBytes);
//
//        // Αποθήκευση metadata στη βάση δεδομένων
//        DocumentFile doc = new DocumentFile();
//        doc.setFilename(filename);
//        doc.setContentType(contentType);
//        doc.setFileSize((long) pdfBytes.length);
//        doc.setSha256(sha256);
//
//        db.dbTransactions.storeObject(doc);
//        Long docId = doc.getDocumentId();
//
//        // Αποθήκευση αρχείου στο filesystem
//        String storagePath = FileStorageService.saveFile(docId, pdfBytes);
//        doc.setStoragePath(storagePath);
//        db.dbTransactions.storeWithMergeObject(doc); // Ενημέρωση με διαδρομή αποθήκευσης
//
//        return docId;
//    }

    public static DocumentSignature signDocument(
            DocumentFile doc, String scheme, Tbluser user, char[] keystorePassword) throws Exception {

        if (user == null || user.getUsername() == null) {
            throw new IllegalArgumentException("User is required for signing");
        }
        if (keystorePassword == null || keystorePassword.length == 0) {
            throw new IllegalArgumentException("Keystore password is required for signing");
        }

        DocumentSignature sig = new DocumentSignature();
        sig.setDocumentId(doc);
        sig.setIdUser(user);
        sig.setScheme(scheme);
        sig.setSignTime(new java.util.Date());

        try {
            PdfSignatureEmbedder.embedSignature(doc.getDocumentId(), sig, user.getUsername(), keystorePassword);
        } catch (Exception e) {
            // Καταγραφή warning αλλά μην αποτύχει - η υπογραφή είναι ακόμα στη βάση δεδομένων
            System.err.println("Warning: Αποτυχία ενσωμάτωσης υπογραφής στο PDF: " + e.getMessage());
            e.printStackTrace();
        }

        // ΚΡΙΣΙΜΟ: Ελέγχος αν οι υπογραφές αποθηκεύτηκαν σωστά
        System.out.println("DEBUG signDocument: After embedSignature - RSA: " +
                (sig.getRsaSignature() != null ? sig.getRsaSignature().length + " bytes" : "null") +
                ", PQC: " + (sig.getPqcSignature() != null ? sig.getPqcSignature().length + " bytes" : "null"));

        return sig;
    }

    public VerificationResult verify(Long docId) throws Exception {
        if (docId == null) {
            throw new IllegalArgumentException("Document ID cannot be null");
        }

        DocumentFile doc = db.dbTransactions.getObjectById(DocumentFile.class, docId);
        if (doc == null) {
            throw new IllegalArgumentException("Document not found: " + docId);
        }

        // ΚΡΙΣΙΜΟ: Διαβάζουμε τις υπογραφές από τη βάση δεδομένων
        // Το saveIncremental δεν αποθηκεύει custom metadata, οπότε δεν μπορούμε να βασιζόμαστε στο PDF metadata
        DocumentSignature sig = findLatestSignatureForDoc(docId);
        if (sig == null) {
            throw new IllegalArgumentException("Δεν βρέθηκε υπογραφή για το έγγραφο: " + docId);
        }

        // Ορισμός αναφοράς εγγράφου
        sig.setDocumentId(doc);

        System.out.println("DEBUG Verify: Read signature from database - RSA: " +
                (sig.getRsaSignature() != null ? sig.getRsaSignature().length + " bytes" : "null") +
                ", PQC: " + (sig.getPqcSignature() != null ? sig.getPqcSignature().length + " bytes" : "null") +
                ", Scheme: " + sig.getScheme());

        // Φόρτωση δημόσιων κλειδιών του υπογράφοντα (αν idUser υπάρχει), αλλιώς global για παλιές υπογραφές
        PublicKey rsaPublic;
        PublicKey pqPublic;
        Tbluser signer = sig.getIdUser();
        if (signer != null && signer.getUsername() != null) {
            String username = signer.getUsername();
            rsaPublic = KeyLoader.loadRsaPublicKeyForUser(username);
            pqPublic = KeyLoader.loadPqcPublicKeyForUser(username);
            System.out.println("DEBUG Verify: Using signer keys for user: " + username);
        } else {
            rsaPublic = KeyLoader.loadPublicKey("data/rsa-public.key", "RSA");
            pqPublic = KeyLoader.loadPublicKey("data/pqc-public.key", "DILITHIUM");
            System.out.println("DEBUG Verify: Using global keys (legacy signature without idUser)");
        }

        // ΚΡΙΣΙΜΟ: Επαλήθευση υπογραφής με βάση το ByteRange του PDF
        // Αυτή είναι η σωστή προσέγγιση για PDF signature verification
        // 1. Βρίσκουμε το ByteRange από το PDF signature field
        // 2. Παίρνουμε τα bytes που ορίζει το ByteRange (αυτά που υπογράφηκαν)
        // 3. Επαληθεύουμε τις υπογραφές (RSA/PQC) πάνω σε αυτά τα bytes
        // 4. Ελέγχουμε αν υπάρχουν incremental updates μετά την υπογραφή

        Path filePath = FileStorageService.getFilePath(docId);
        String pdfPath = filePath.toString();

        try {
            // Χρήση PdfSignatureVerifier για επαλήθευση με ByteRange
            PdfSignatureVerifier.SignatureVerificationResult result =
                    PdfSignatureVerifier.verifySignatures(pdfPath, sig, rsaPublic, pqPublic);

            return new VerificationResult(
                    result.rsaOk,
                    result.pqcOk,
                    result.overall,
                    sig,
                    doc,
                    result.hasIncrementalUpdates,
                    result.coversWholeFile
            );
        } catch (Exception e) {
            System.err.println("Warning: ByteRange verification failed, falling back to hash-based: " + e.getMessage());
            e.printStackTrace();

            // Fallback: Αν δεν υπάρχει PDF cryptographic signature με ByteRange,
            // χρησιμοποιούμε την παλιά λογική με hash

            String originalHash = doc.getSha256();
            if (originalHash == null || originalHash.isEmpty()) {
                throw new IllegalArgumentException("Το έγγραφο δεν έχει SHA-256 hash. Δεν μπορεί να επαληθευτεί.");
            }

            byte[] hashBytes = hexStringToByteArray(originalHash);
            byte[] dataToVerify = hashBytes;

            Boolean rsaOk = null;
            Boolean pqcOk = null;

            if (sig.getRsaSignature() != null) {
                try {
                    Signature rsa = Signature.getInstance("SHA256withRSA");
                    rsa.initVerify(rsaPublic);
                    rsa.update(dataToVerify);
                    rsaOk = rsa.verify(sig.getRsaSignature());
                } catch (Exception ex) {
                    System.err.println("Warning: RSA verification failed: " + ex.getMessage());
                    rsaOk = false;
                }
            }

            if (sig.getPqcSignature() != null) {
                try {
                    Signature pq = Signature.getInstance("DILITHIUM3", "BC");
                    pq.initVerify(pqPublic);
                    pq.update(dataToVerify);
                    pqcOk = pq.verify(sig.getPqcSignature());
                } catch (Exception ex) {
                    System.err.println("Warning: PQC verification failed: " + ex.getMessage());
                    pqcOk = false;
                }
            }

            boolean overall = (rsaOk == null || rsaOk) && (pqcOk == null || pqcOk);
            return new VerificationResult(rsaOk, pqcOk, overall, sig, doc);
        }
    }

    /**
     * Μετατρέπει hex string σε byte array.
     */
    private static byte[] hexStringToByteArray(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i+1), 16));
        }
        return data;
    }

    @SuppressWarnings("unchecked")
    private DocumentSignature findLatestSignatureForDoc(Long docId) {
        EntityManager entityManager = null;
        try {
            entityManager = JPAUtil.getEntityManagerFactory().createEntityManager();
            entityManager.getTransaction().begin();
            Query query = entityManager.createNamedQuery("DocumentSignature.findByDocumentId");
            query.setParameter("docId", docId);
            query.setMaxResults(1);
            List<DocumentSignature> results = (List<DocumentSignature>) query.getResultList();
            entityManager.getTransaction().commit();
            DocumentSignature sig = results.isEmpty() ? null : results.get(0);

            if (sig != null) {
                System.out.println("DEBUG findLatestSignatureForDoc: Found signature ID: " + sig.getSignatureId() +
                        ", Scheme: " + sig.getScheme() +
                        ", RSA: " + (sig.getRsaSignature() != null ? sig.getRsaSignature().length + " bytes" : "null") +
                        ", PQC: " + (sig.getPqcSignature() != null ? sig.getPqcSignature().length + " bytes" : "null"));
            } else {
                System.out.println("DEBUG findLatestSignatureForDoc: No signature found in database for docId: " + docId);
            }

            return sig;
        } catch (Exception e) {
            if (entityManager != null && entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            throw new RuntimeException("Error finding signature for document: " + docId, e);
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }
    }

    public static class VerificationResult {
        public final Boolean rsaOk;
        public final Boolean pqcOk;
        public final boolean overall;
        public final DocumentSignature signature;
        public final DocumentFile document;
        public final Boolean hasIncrementalUpdates;
        public final Boolean coversWholeFile;

        public VerificationResult(Boolean rsaOk, Boolean pqcOk, boolean overall, DocumentSignature signature, DocumentFile document) {
            this(rsaOk, pqcOk, overall, signature, document, null, null);
        }

        public VerificationResult(Boolean rsaOk, Boolean pqcOk, boolean overall, DocumentSignature signature,
                                  DocumentFile document, Boolean hasIncrementalUpdates, Boolean coversWholeFile) {
            this.rsaOk = rsaOk;
            this.pqcOk = pqcOk;
            this.overall = overall;
            this.signature = signature;
            this.document = document;
            this.hasIncrementalUpdates = hasIncrementalUpdates;
            this.coversWholeFile = coversWholeFile;
        }
    }
}
