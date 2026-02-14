package utils;

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

import java.io.*;
import java.text.SimpleDateFormat;

public class PdfVisualSignatureUtil {

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

                // background
                cs.setNonStrokingColor(242, 242, 242);
                cs.addRect(0, 0, w, h);
                cs.fill();

                // border
                cs.setStrokingColor(77, 77, 77);
                cs.setLineWidth(1f);
                cs.addRect(0, 0, w, h);
                cs.stroke();

                float tx = 10;
                float ty = h - 18;

                // title
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
                cs.setNonStrokingColor(0, 102, 0);
                cs.newLineAtOffset(tx, ty);
                cs.showText("DIGITAL SIGNATURE");
                cs.endText();

                ty -= 16;

                // scheme
                String scheme = sig.getScheme() == null ? "" : sig.getScheme();
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 10);
                cs.setNonStrokingColor(0, 0, 0);
                cs.newLineAtOffset(tx, ty);
                cs.showText("Scheme: " + scheme);
                cs.endText();

                ty -= 14;

                // date
                if (sig.getSignTime() != null) {
                    String dt = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(sig.getSignTime());
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA, 9);
                    cs.setNonStrokingColor(77, 77, 77);
                    cs.newLineAtOffset(tx, ty);
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
