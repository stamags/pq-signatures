package utils;

import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface;
import signatures.KeyLoader;

import java.io.IOException;
import java.io.InputStream;
import java.security.PrivateKey;
import java.security.Signature;

/**
 * SignatureInterface implementation for RSA signing using PDFBox.
 * This creates a proper PDF cryptographic signature (PAdES compatible).
 */
public class RsaSignatureInterface implements SignatureInterface {

    private final PrivateKey privateKey;

    public RsaSignatureInterface() throws Exception {
        this.privateKey = KeyLoader.loadPrivateKey("data/rsa-private.key", "RSA");
    }

    @Override
    public byte[] sign(InputStream content) throws IOException {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);

            // Read content and update signature
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = content.read(buffer)) != -1) {
                signature.update(buffer, 0, bytesRead);
            }

            return signature.sign();
        } catch (Exception e) {
            throw new IOException("Failed to sign content", e);
        }
    }
}
