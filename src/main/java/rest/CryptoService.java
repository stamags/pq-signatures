package rest;

import model.DocumentFile;
import model.DocumentSignature;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import signatures.KeyLoader;
import utils.FileStorageService;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;

public class CryptoService {

    static {
        Security.addProvider(new BouncyCastleProvider());
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

            Signature pq = Signature.getInstance("DILITHIUM3", "BC");
            pq.initSign(pqPriv);
            pq.update(data);
            sig.setPqcSignature(pq.sign());
        }

        return sig;
    }

    public static CryptoService.VerificationResult verifyDocument(
            DocumentFile doc, DocumentSignature sig) throws Exception {

        // Read file from filesystem
        byte[] data = FileStorageService.readFile(doc.getDocumentId());

        Boolean rsaOk = null;
        Boolean pqcOk = null;

        if (sig.getRsaSignature() != null) {
            PublicKey rsaPub =
                    KeyLoader.loadPublicKey("data/rsa-public.key", "RSA");

            Signature rsa = Signature.getInstance("SHA256withRSA");
            rsa.initVerify(rsaPub);
            rsa.update(data);
            rsaOk = rsa.verify(sig.getRsaSignature());
        }

        if (sig.getPqcSignature() != null) {
            PublicKey pqPub =
                    KeyLoader.loadPublicKey("data/pqc-public.key", "DILITHIUM");

            Signature pq = Signature.getInstance("DILITHIUM3", "BC");
            pq.initVerify(pqPub);
            pq.update(data);
            pqcOk = pq.verify(sig.getPqcSignature());
        }

        boolean overall =
                (rsaOk == null || rsaOk) &&
                        (pqcOk == null || pqcOk);

        return new VerificationResult(rsaOk, pqcOk, overall);
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
