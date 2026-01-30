package rest;

import model.DocumentSignature;
import model.DocumentFile;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import signatures.KeyLoader;
import db.JPAUtil;
import utils.FileStorageService;
import utils.PdfSignatureEmbedder;

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

    public Long saveDocument(String filename, String contentType, byte[] pdfBytes) throws Exception {
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be null or empty");
        }
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new IllegalArgumentException("PDF bytes cannot be null or empty");
        }

        // Υπολογισμός SHA-256
        String sha256 = FileStorageService.calculateSha256(pdfBytes);

        // Αποθήκευση metadata στη βάση δεδομένων
        DocumentFile doc = new DocumentFile();
        doc.setFilename(filename);
        doc.setContentType(contentType);
        doc.setFileSize((long) pdfBytes.length);
        doc.setSha256(sha256);

        db.dbTransactions.storeObject(doc);
        Long docId = doc.getDocumentId();

        // Αποθήκευση αρχείου στο filesystem
        String storagePath = FileStorageService.saveFile(docId, pdfBytes);
        doc.setStoragePath(storagePath);
        db.dbTransactions.storeWithMergeObject(doc); // Ενημέρωση με διαδρομή αποθήκευσης

        return docId;
    }

    public static DocumentSignature signDocument(
            DocumentFile doc, String scheme) throws Exception {

        // Ανάγνωση αρχείου από το filesystem
        byte[] data = FileStorageService.readFile(doc.getDocumentId());

        DocumentSignature sig = new DocumentSignature();
        sig.setDocumentId(doc);
        sig.setScheme(scheme);

        if ("RSA".equals(scheme) || "HYBRID".equals(scheme)) {
            PrivateKey rsaPriv =
                    KeyLoader.loadPrivateKey("data/rsa-private.key", "RSA");

            Signature rsa = Signature.getInstance("SHA256withRSA");
            rsa.initSign(rsaPriv);
            rsa.update(data);
            sig.setRsaSignature(rsa.sign());
        }

        if ("DILITHIUM".equals(scheme) || "HYBRID".equals(scheme)) {
            PrivateKey pqPriv =
                    KeyLoader.loadPrivateKey("data/pqc-private.key", "DILITHIUM");

            Signature pq = Signature.getInstance("DILITHIUM3", "BC");
            pq.initSign(pqPriv);
            pq.update(data);
            sig.setPqcSignature(pq.sign());
        }

        // Ενσωμάτωση υπογραφής στο αρχείο PDF
        try {
            PdfSignatureEmbedder.embedSignature(doc.getDocumentId(), sig);
        } catch (Exception e) {
            // Καταγραφή warning αλλά μην αποτύχει - η υπογραφή είναι ακόμα στη βάση δεδομένων
            System.err.println("Warning: Αποτυχία ενσωμάτωσης υπογραφής στο PDF: " + e.getMessage());
            e.printStackTrace();
        }

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

        // Προσπάθεια ανάγνωσης υπογραφής από PDF πρώτα (ενσωματωμένη), αλλιώς από τη βάση δεδομένων
        DocumentSignature sig = null;
        try {
            sig = PdfSignatureEmbedder.readEmbeddedSignature(docId);
            if (sig != null) {
                // Ορισμός αναφοράς εγγράφου για ενσωματωμένη υπογραφή
                sig.setDocumentId(doc);
            }
        } catch (Exception e) {
            // Αν η ανάγνωση από PDF αποτύχει, συνεχίζουμε με αναζήτηση στη βάση δεδομένων
            System.err.println("Info: Δεν ήταν δυνατή η ανάγνωση ενσωματωμένης υπογραφής από PDF: " + e.getMessage());
        }

        // Fallback στη βάση δεδομένων αν δεν βρεθεί ενσωματωμένη υπογραφή
        if (sig == null) {
            sig = findLatestSignatureForDoc(docId);
            if (sig == null) {
                throw new IllegalArgumentException("Δεν βρέθηκε υπογραφή για το έγγραφο: " + docId);
            }
        }

        PublicKey rsaPublic = KeyLoader.loadPublicKey("data/rsa-public.key", "RSA");
        PublicKey pqPublic  = KeyLoader.loadPublicKey("data/pqc-public.key", "DILITHIUM");

        // Ανάγνωση αρχείου από το filesystem
        byte[] data = FileStorageService.readFile(docId);

        Boolean rsaOk = null;
        Boolean pqcOk = null;

        if (sig.getRsaSignature() != null) {
            Signature rsa = Signature.getInstance("SHA256withRSA");
            rsa.initVerify(rsaPublic);
            rsa.update(data);
            rsaOk = rsa.verify(sig.getRsaSignature());
        }

        if (sig.getPqcSignature() != null) {
            Signature pq = Signature.getInstance("DILITHIUM3", "BC");
            pq.initVerify(pqPublic);
            pq.update(data);
            pqcOk = pq.verify(sig.getPqcSignature());
        }

        boolean overall =
                (rsaOk == null || rsaOk) &&
                        (pqcOk == null || pqcOk);

        return new VerificationResult(rsaOk, pqcOk, overall);
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
            return results.isEmpty() ? null : results.get(0);
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

        public VerificationResult(Boolean rsaOk, Boolean pqcOk, boolean overall) {
            this.rsaOk = rsaOk;
            this.pqcOk = pqcOk;
            this.overall = overall;
        }
    }
}
