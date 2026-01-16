package rest;

import model.DocumentSignature;
import model.DocumentFile;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import signatures.KeyLoader;
import db.JPAUtil;
import utils.FileStorageService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.security.*;
import java.util.List;

public class DocumentService {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public DocumentService() {
        // Default constructor - uses static dbTransactions methods
    }

    public Long saveDocument(String filename, String contentType, byte[] pdfBytes) throws Exception {
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("Το αρχείο δεν μπορεί να είναι κενό");
        }
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new IllegalArgumentException("Τα PDF bytes δεν μπορεί να είναι κενά");
        }
        // Calculate SHA-256
        String sha256 = FileStorageService.calculateSha256(pdfBytes);
        
        // Save metadata to DB
        DocumentFile doc = new DocumentFile();
        doc.setFilename(filename);
        doc.setContentType(contentType);
        doc.setFileSize((long) pdfBytes.length);
        doc.setSha256(sha256);

        db.dbTransactions.storeObject(doc);
        Long docId = doc.getDocumentId();
        
        // Save file to filesystem
        String storagePath = FileStorageService.saveFile(docId, pdfBytes);
        doc.setStoragePath(storagePath);
        db.dbTransactions.storeWithMergeObject(doc); // Update with storage path
        
        return docId;
    }

    public static DocumentSignature signDocument(
            DocumentFile doc, String scheme) throws Exception {

        // Read file from filesystem
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

            Signature pq = Signature.getInstance("DILITHIUM", "BCPQC");
            pq.initSign(pqPriv);
            pq.update(data);
            sig.setPqcSignature(pq.sign());
        }

        return sig;
    }

//    public Long signDocument(Long docId, String scheme) throws Exception {
//        if (docId == null) {
//            throw new IllegalArgumentException("Το Document ID δεν μπορεί να είναι κενό");
//        }
//        if (scheme == null || scheme.trim().isEmpty()) {
//            throw new IllegalArgumentException("Το Signature scheme δεν μπορεί να είναι κενό");
//        }
//
//        DocumentFile doc = db.dbTransactions.getObjectById(DocumentFile.class, docId);
//        if (doc == null) {
//            throw new IllegalArgumentException("Το αρχείο δεν βρεθηκε.ID αρχείου : " + docId);
//        }
//
//
//
//        // keys from files (όπως ήδη κάνεις)
//        PrivateKey rsaPrivate = KeyLoader.loadPrivateKey("data/rsa-private.key", "RSA");
//        PrivateKey pqPrivate  = KeyLoader.loadPrivateKey("data/pqc-private.key", "DILITHIUM");
//
//        // Read file from filesystem
//        byte[] data = FileStorageService.readFile(docId);
//
//
//        DocumentSignature sig = new DocumentSignature();
//        sig.setDocumentId(doc);
//        sig.setScheme(scheme);
//
//        if ("RSA".equalsIgnoreCase(scheme) || "HYBRID".equalsIgnoreCase(scheme)) {
//            Signature rsa = Signature.getInstance("SHA256withRSA");
//            rsa.initSign(rsaPrivate);
//            rsa.update(data);
//            sig.setRsaSignature(rsa.sign());
////            sig.setRsaAlg("SHA256withRSA");
//        }
//
//        if ("DILITHIUM".equalsIgnoreCase(scheme) || "HYBRID".equalsIgnoreCase(scheme)) {
//            Signature pq = Signature.getInstance("DILITHIUM3", "BC");
//            pq.initSign(pqPrivate);
//            pq.update(data);
//            sig.setPqcSignature(pq.sign());
////            sig.setPqcAlg("DILITHIUM3");
//        }
//
//        db.dbTransactions.storeObject(sig);
//        return sig.getSignatureId();
//    }

    public VerificationResult verify(Long docId) throws Exception {
        if (docId == null) {
            throw new IllegalArgumentException("Το Document ID δεν μπορεί να είναι κενό");
        }
        DocumentFile doc = db.dbTransactions.getObjectById(DocumentFile.class, docId);
        if (doc == null) {
            throw new IllegalArgumentException("Δεν βρέθηκε το έγγραφο με ID  :  " + docId);
        }

        // Θα παίρνεις την τελευταία signature εγγραφή (π.χ. με query)
        DocumentSignature sig = findLatestSignatureForDoc(docId);
        if (sig == null) {
            throw new IllegalArgumentException("Δεν βρέθηκε υπογραφή για το έγγραφο με ID :   " + docId);
        }

        PublicKey rsaPublic = KeyLoader.loadPublicKey("data/rsa-public.key", "RSA");
        PublicKey pqPublic  = KeyLoader.loadPublicKey("data/pqc-public.key", "DILITHIUM");

        // Read file from filesystem
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
