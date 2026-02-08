package utils;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;

import java.io.*;
import java.util.Set;

public class PdfCanonicalUtil {

    private static final Set<String> SIG_KEYS = Set.of(
            "RSA_SIGNATURE",
            "DILITHIUM_SIGNATURE",
            "SIGNATURE_SCHEME",
            "SIGNATURE_TIME"
    );

    public static byte[] canonicalBytesWithoutSignatures(String pdfPath) throws Exception {
        try (PDDocument doc = PDDocument.load(new File(pdfPath));
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            PDDocumentInformation info = doc.getDocumentInformation();

            // αφαιρούμε/μηδενίζουμε τα signature metadata fields
            for (String k : SIG_KEYS) {
                info.setCustomMetadataValue(k, null);
            }
            doc.setDocumentInformation(info);

            // Αφαίρεση signature annotations που προστέθηκαν κατά την υπογραφή
            // Αυτό βοηθάει να πάρουμε το PDF όπως ήταν πριν την υπογραφή
            try {
                int pageCount = doc.getNumberOfPages();
                for (int i = 0; i < pageCount; i++) {
                    var page = doc.getPage(i);
                    var annotations = page.getAnnotations();
                    if (annotations != null) {
                        // Αφαίρεση annotations που σχετίζονται με υπογραφές
                        annotations.removeIf(ann ->
                                ann instanceof org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationText &&
                                        "DigitalSignature".equals(ann.getAnnotationName())
                        );
                    }
                }
            } catch (Exception e) {
                // Αν αποτύχει η αφαίρεση annotations, συνεχίζουμε
                System.err.println("Warning: Δεν ήταν δυνατή η αφαίρεση annotations: " + e.getMessage());
            }

            // σώζουμε σε bytes (canonical form για τον δικό μας αλγόριθμο)
            doc.save(baos);
            return baos.toByteArray();
        }
    }
}
