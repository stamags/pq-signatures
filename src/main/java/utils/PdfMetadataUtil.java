package utils;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;

import java.io.File;
import java.util.Map;

public class PdfMetadataUtil {

    public static void writeMetadata(
            String pdfPath,
            Map<String, String> entries
    ) throws Exception {

        PDDocument doc = PDDocument.load(new File(pdfPath));
        PDDocumentInformation info = doc.getDocumentInformation();

        for (Map.Entry<String, String> e : entries.entrySet()) {
            info.setCustomMetadataValue(e.getKey(), e.getValue());
        }

        doc.setDocumentInformation(info);
        doc.save(pdfPath);
        doc.close();
    }

    public static String readMetadata(String pdfPath, String key)
            throws Exception {

        PDDocument doc = PDDocument.load(new File(pdfPath));
        PDDocumentInformation info = doc.getDocumentInformation();

        String value = info.getCustomMetadataValue(key);
        doc.close();

        return value;
    }
}
