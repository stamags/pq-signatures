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

public class PdfSignatureEmbedder {

    private PdfSignatureEmbedder() {}

    /**
     * External signing + visible signature + raw RSA/PQC πάνω στα ByteRange bytes.
     */
    public static void embedSignature(String pdfPath, DocumentSignature signature) throws Exception {
        addPdfCryptographicSignature(pdfPath, signature);
    }

    public static void embedSignature(Long documentId, DocumentSignature signature) throws Exception {
        Path filePath = FileStorageService.getFilePath(documentId);
        if (!Files.exists(filePath)) {
            throw new Exception("PDF file not found for document ID: " + documentId);
        }
        embedSignature(filePath.toString(), signature);
    }

    private static void addPdfCryptographicSignature(String pdfPath, DocumentSignature signature) throws Exception {
        Path original = Paths.get(pdfPath);
        Path temp = Files.createTempFile("pdf_signed_", ".pdf");

        try (PDDocument doc = PDDocument.load(original.toFile())) {

            // 1) Signature dictionary
            PDSignature pdSignature = new PDSignature();

            Calendar cal = Calendar.getInstance();
            if (signature.getSignTime() != null) {
                cal.setTime(signature.getSignTime());
            }
            pdSignature.setSignDate(cal);

            pdSignature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
            pdSignature.setSubFilter(PDSignature.SUBFILTER_ETSI_CADES_DETACHED);

            String scheme = normalizeScheme(signature.getScheme());
            String name = "PQ-Signatures System" + (scheme.isEmpty() ? "" : " (" + scheme + ")");
            pdSignature.setName(name);
            pdSignature.setReason("Digital Signature");
            pdSignature.setLocation("Greece");

            // 2) SignatureOptions + visible signature
            SignatureOptions options = new SignatureOptions();
            options.setPreferredSignatureSize(150000);
            options.setPage(0);

            // Θέση οπτικού κουτιού (κάτω-αριστερά)
            PDRectangle pageSize = doc.getPage(0).getMediaBox();

            float margin = 20f;
            float width = 250f;
            float height = 80f;

            float x = margin;
            float y = pageSize.getHeight() - height - margin;

            PDRectangle rect = new PDRectangle(x, y, width, height);

            options.setVisualSignature(
                    PdfVisualSignatureUtil.createVisualSignatureTemplate(doc, 0, rect, signature)
            );

            // 3) Prepare document for external signing (ΜΗΝ βάλεις SignatureInterface εδώ)
            doc.addSignature(pdSignature, (SignatureInterface) null, options);

            // 4) External signing session -> temp output
            try (FileOutputStream fos = new FileOutputStream(temp.toFile())) {
                ExternalSigningSupport externalSigning = doc.saveIncrementalForExternalSigning(fos);

                byte[] bytesToSign = externalSigning.getContent().readAllBytes();

                // Debug SHA-256 (προαιρετικό)
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] hash = md.digest(bytesToSign);
                System.out.println("DEBUG ByteRange bytes len=" + bytesToSign.length +
                        ", SHA-256(first16)=" + toHex(Arrays.copyOf(hash, 16)));

                // 5) Raw RSA/PQC signatures (για DB verify)
                signRawBytesForDatabase(signature, scheme, bytesToSign);

                // 6) CMS/PKCS#7 signature bytes για PDF /Contents
                // Απαιτεί δική σου υλοποίηση RsaSignatureInterface (όπως ήδη έχεις)
                RsaSignatureInterface signatureInterface = new RsaSignatureInterface();
                byte[] cmsSignature = signatureInterface.sign(new ByteArrayInputStream(bytesToSign));

                // 7) Write CMS into PDF
                externalSigning.setSignature(cmsSignature);
            }
        }

        // Replace original with signed
        Files.move(temp, original, StandardCopyOption.REPLACE_EXISTING);
    }

    private static void signRawBytesForDatabase(DocumentSignature signatureObj, String scheme, byte[] bytesToSign) throws Exception {
        // RSA
        if ("RSA".equals(scheme) || "HYBRID".equals(scheme)) {
            PrivateKey rsaPriv = KeyLoader.loadPrivateKey("data/rsa-private.key", "RSA");
            Signature rsa = Signature.getInstance("SHA256withRSA");
            rsa.initSign(rsaPriv);
            rsa.update(bytesToSign);
            signatureObj.setRsaSignature(rsa.sign());
            System.out.println("DEBUG Raw RSA signature len=" + signatureObj.getRsaSignature().length);
        } else {
            signatureObj.setRsaSignature(null);
        }

        // PQC
        if ("DILITHIUM".equals(scheme) || "HYBRID".equals(scheme)) {
            PrivateKey pqPriv = KeyLoader.loadPrivateKey("data/pqc-private.key", "DILITHIUM");
            Signature pq = Signature.getInstance("DILITHIUM3", "BC");
            pq.initSign(pqPriv);
            pq.update(bytesToSign);
            signatureObj.setPqcSignature(pq.sign());
            System.out.println("DEBUG Raw PQC signature len=" + signatureObj.getPqcSignature().length);
        } else {
            signatureObj.setPqcSignature(null);
        }
    }

    private static String normalizeScheme(String s) {
        return s == null ? "" : s.trim().toUpperCase();
    }

    private static String toHex(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (byte v : b) sb.append(String.format("%02x", v));
        return sb.toString();
    }
}
