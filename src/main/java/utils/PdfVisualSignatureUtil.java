package utils;

import beans.LoginBean;
import jakarta.inject.Inject;
import model.DocumentSignature;
import model.Tbluser;
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

                // Όνομα υπογράφοντα: όνομα + επίθετο από idUser (μεταγραφή σε Latin για Helvetica)
                String signerDisplay = toLatinForPdf(signerDisplayName(sig));
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 10);
                cs.setNonStrokingColor(0, 0, 0);
                cs.newLineAtOffset(textX, ty);
                cs.showText("User: " + (signerDisplay == null ? "" : signerDisplay));
                cs.endText();

                ty -= 14;

                // Scheme (μεταγραφή για Helvetica)
                String scheme = toLatinForPdf(sig.getScheme() == null ? "" : sig.getScheme());
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 10);
                cs.setNonStrokingColor(0, 0, 0);
                cs.newLineAtOffset(textX, ty);
                cs.showText("Scheme: " + scheme);
                cs.endText();

                ty -= 14;

                // Date (μόνο αριθμοί/λατινικά)
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

    /** Όνομα + επίθετο υπογράφοντα από idUser, αλλιώς username, αλλιώς "User". */
    private static String signerDisplayName(DocumentSignature sig) {
        if (sig == null || sig.getIdUser() == null) {
            return "User";
        }
        Tbluser user = sig.getIdUser();
        String name = user.getName() != null ? user.getName().trim() : "";
        String surname = user.getSurname() != null ? user.getSurname().trim() : "";
        String display = (name + " " + surname).trim();
        if (!display.isEmpty()) {
            return display;
        }
        return user.getUsername() != null ? user.getUsername() : "User";
    }

    /**
     * Μετατρέπει κείμενο σε Latin/ASCII ώστε να εμφανίζεται σωστά με Helvetica (WinAnsiEncoding).
     * Ελληνικά γράμματα μεταγράφονται σε Latin (π.χ. Π→P, α→a). Public για χρήση και από PdfSignatureEmbedder.
     */
    public static String toLatinForPdf(String text) {
        if (text == null || text.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 0x20 && c <= 0x7E) {
                sb.append(c);
                continue;
            }
            switch (c) {
                case 'Α': sb.append('A'); break;
                case 'Β': sb.append('B'); break;
                case 'Γ': sb.append('G'); break;
                case 'Δ': sb.append('D'); break;
                case 'Ε': sb.append('E'); break;
                case 'Ζ': sb.append('Z'); break;
                case 'Η': sb.append('H'); break;
                case 'Θ': sb.append("Th"); break;
                case 'Ι': sb.append('I'); break;
                case 'Κ': sb.append('K'); break;
                case 'Λ': sb.append('L'); break;
                case 'Μ': sb.append('M'); break;
                case 'Ν': sb.append('N'); break;
                case 'Ξ': sb.append('X'); break;
                case 'Ο': sb.append('O'); break;
                case 'Π': sb.append('P'); break;
                case 'Ρ': sb.append('R'); break;
                case 'Σ': sb.append('S'); break;
                case 'Τ': sb.append('T'); break;
                case 'Υ': sb.append('Y'); break;
                case 'Φ': sb.append('F'); break;
                case 'Χ': sb.append("Ch"); break;
                case 'Ψ': sb.append("Ps"); break;
                case 'Ω': sb.append('O'); break;
                case 'α': sb.append('a'); break;
                case 'β': sb.append('b'); break;
                case 'γ': sb.append('g'); break;
                case 'δ': sb.append('d'); break;
                case 'ε': sb.append('e'); break;
                case 'ζ': sb.append('z'); break;
                case 'η': sb.append('h'); break;
                case 'θ': sb.append("th"); break;
                case 'ι': sb.append('i'); break;
                case 'κ': sb.append('k'); break;
                case 'λ': sb.append('l'); break;
                case 'μ': sb.append('m'); break;
                case 'ν': sb.append('n'); break;
                case 'ξ': sb.append('x'); break;
                case 'ο': sb.append('o'); break;
                case 'π': sb.append('p'); break;
                case 'ρ': sb.append('r'); break;
                case 'σ': sb.append('s'); break;
                case 'ς': sb.append('s'); break;
                case 'τ': sb.append('t'); break;
                case 'υ': sb.append('u'); break;
                case 'φ': sb.append('f'); break;
                case 'χ': sb.append("ch"); break;
                case 'ψ': sb.append("ps"); break;
                case 'ω': sb.append('o'); break;
                case 'ά': sb.append('a'); break;
                case 'έ': sb.append('e'); break;
                case 'ή': sb.append('h'); break;
                case 'ί': sb.append('i'); break;
                case 'ό': sb.append('o'); break;
                case 'ύ': sb.append('u'); break;
                case 'ώ': sb.append('o'); break;
                case 'ϊ': sb.append('i'); break;
                case 'ϋ': sb.append('u'); break;
                case 'ΐ': sb.append('i'); break;
                case 'ΰ': sb.append('u'); break;
                default: sb.append('?');
            }
        }
        return sb.toString();
    }
}
