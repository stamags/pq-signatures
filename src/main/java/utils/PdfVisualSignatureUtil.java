package utils;

import beans.LoginBean;
import jakarta.inject.Inject;
import model.DocumentSignature;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceDictionary;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceEntry;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceStream;
import org.apache.pdfbox.pdmodel.interactive.form.*;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.glassfish.jaxb.core.v2.TODO;


import java.io.*;
import java.text.SimpleDateFormat;




public class PdfVisualSignatureUtil {

    @Inject
    private LoginBean loginBean;

    private PdfVisualSignatureUtil() {}

    /**
     * Template που απαιτεί το PDFBox για visible signature:
     * - 1 page
     * - AcroForm
     * - 1 Signature field (π.χ. "Signature1")
     * - Widget annotation στη σελίδα
     * - Appearance (/AP) για να "φαίνεται" το κουτί/κείμενο
     */
    public static InputStream createVisualSignatureTemplate(
            PDDocument sourceDoc,
            int pageIndex,
            PDRectangle rect,
            DocumentSignature sig
    ) throws IOException {

        try (PDDocument tmpl = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            // 1) Page με ίδιο μέγεθος με το source
            PDPage srcPage = sourceDoc.getPage(pageIndex);
            PDPage page = new PDPage(srcPage.getMediaBox());
            tmpl.addPage(page);

            // 2) AcroForm
            PDAcroForm acroForm = new PDAcroForm(tmpl);
            tmpl.getDocumentCatalog().setAcroForm(acroForm);

            // resources (για να μπορεί να ζωγραφίσει text)
            PDResources dr = new PDResources();
            dr.put(COSName.getPDFName("F1"), PDType1Font.HELVETICA);
            dr.put(COSName.getPDFName("F2"), PDType1Font.HELVETICA_BOLD);
            acroForm.setDefaultResources(dr);
            acroForm.setDefaultAppearance("/F1 10 Tf 0 g");

            // 3) Signature field + widget
            PDSignatureField sigField = new PDSignatureField(acroForm);
            sigField.setPartialName("Signature1"); // ένα όνομα είναι αρκετό

            PDAnnotationWidget widget = sigField.getWidgets().get(0);
            widget.setRectangle(rect);
            widget.setPage(page);
            page.getAnnotations().add(widget);

            // add field to acroForm
            acroForm.getFields().add(sigField);

            // 4) Appearance stream (το “κουτί” που φαίνεται)
            PDAppearanceStream apStream = new PDAppearanceStream(tmpl);
            apStream.setBBox(new PDRectangle(rect.getWidth(), rect.getHeight()));
            apStream.setResources(new PDResources());

            try (PDPageContentStream cs = new PDPageContentStream(tmpl, apStream)) {

                float w = rect.getWidth();
                float h = rect.getHeight();

                // --- SOFT GREY BACKGROUND ---
                cs.setNonStrokingColor(235, 235, 235);
                cs.addRect(0, 0, w, h);
                cs.fill();

// --- BORDER ---
                cs.setStrokingColor(180, 180, 180);
                cs.setLineWidth(1f);
                cs.addRect(0, 0, w, h);
                cs.stroke();

                // --- LOGO (από resources) ---
                byte[] logoBytes;
                try (InputStream is = PdfVisualSignatureUtil.class.getResourceAsStream("/images/pqc.png")) {
                    if (is == null) throw new IOException("Logo not found in resources: /images/pqc.png");
                    logoBytes = is.readAllBytes();
                }

                PDImageXObject logo = PDImageXObject.createFromByteArray(tmpl, logoBytes, "pqc-logo");
// θέση/μέγεθος μέσα στο box (αριστερά)
                float logoW = 48;
                float logoH = 48;
                float logoX = 10;
                float logoY = h - logoH - 10;

                cs.drawImage(logo, logoX, logoY, logoW, logoH);

                float textX = 10 + logoW + 10;
                float ty = h - 18;


                // Title
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
                cs.setNonStrokingColor(0, 102, 0);
                cs.newLineAtOffset(textX, ty);
                cs.showText("DIGITAL SIGNATURE");
                cs.endText();

                ty -= 16;

                //TODO to username από loginbean
               String username = "George Stamatis";
// Username (από login)
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 10);
                cs.setNonStrokingColor(0, 0, 0);
                cs.newLineAtOffset(textX, ty);
                cs.showText("User: " + (username == null ? "" : username));
                cs.endText();

                ty -= 14;

// Scheme
                String scheme = sig.getScheme() == null ? "" : sig.getScheme();
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 10);
                cs.setNonStrokingColor(0, 0, 0);
                cs.newLineAtOffset(textX, ty);
                cs.showText("Scheme: " + scheme);
                cs.endText();

                ty -= 14;

// Date
                if (sig.getSignTime() != null) {
                    String dt = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(sig.getSignTime());
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA, 9);
                    cs.setNonStrokingColor(77, 77, 77);
                    cs.newLineAtOffset(textX, ty);
                    cs.showText("Date: " + dt);
                    cs.endText();
                }
            }

            PDAppearanceDictionary apDict = new PDAppearanceDictionary();
            apDict.setNormalAppearance(apStream);

            PDAppearanceEntry apEntry = new PDAppearanceEntry(apDict.getCOSObject());
            widget.setAppearance(apDict);

            // 5) save template as stream
            tmpl.save(baos);
            return new ByteArrayInputStream(baos.toByteArray());
        }
    }
}
