package utils;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;

import java.io.*;
import java.util.Set;

public class PdfCanonicalUtil {

    private static final Set<String> SIG_KEYS = Set.of(
            "RSA_SIGNATURE",
            "DILITHIUM_SIGNATURE",
            "SIGNATURE_SCHEME"
    );

    public static byte[] canonicalBytesWithoutSignatures(String pdfPath) throws Exception {
        try (PDDocument doc = PDDocument.load(new File(pdfPath));
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            PDDocumentInformation info = doc.getDocumentInformation();

            // αφαιρούμε/μηδενίζουμε τα signature fields
            for (String k : SIG_KEYS) {
                info.setCustomMetadataValue(k, null);
            }
            doc.setDocumentInformation(info);

            // σώζουμε σε bytes (canonical form για τον δικό μας αλγόριθμο)
            doc.save(baos);
            return baos.toByteArray();
        }
    }
}
