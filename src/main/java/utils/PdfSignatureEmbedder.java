package utils;

import model.DocumentSignature;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationText;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.List;

/**
 * Utility class for embedding signatures into PDF files.
 * Signatures are embedded as Base64-encoded strings in PDF custom metadata.
 */
public class PdfSignatureEmbedder {

    private static final String METADATA_KEY_RSA_SIGNATURE = "RSA_SIGNATURE";
    private static final String METADATA_KEY_PQC_SIGNATURE = "DILITHIUM_SIGNATURE";
    private static final String METADATA_KEY_SCHEME = "SIGNATURE_SCHEME";
    private static final String METADATA_KEY_SIGN_TIME = "SIGNATURE_TIME";

    /**
     * Embeds a signature into a PDF file.
     * Creates a proper PDF cryptographic signature (PAdES) with RSA if available,
     * and also embeds custom metadata for DILITHIUM and hybrid schemes.
     *
     * @param pdfPath Path to the PDF file
     * @param signature The signature to embed
     * @throws Exception if embedding fails
     */
    public static void embedSignature(String pdfPath, DocumentSignature signature) throws Exception {
        // First, add custom metadata and annotations
        try (PDDocument doc = PDDocument.load(new File(pdfPath))) {
            PDDocumentInformation info = doc.getDocumentInformation();

            // Embed scheme
            if (signature.getScheme() != null) {
                info.setCustomMetadataValue(METADATA_KEY_SCHEME, signature.getScheme());
            }

            // Embed RSA signature (Base64 encoded) - for verification purposes
            if (signature.getRsaSignature() != null && signature.getRsaSignature().length > 0) {
                String rsaBase64 = Base64.getEncoder().encodeToString(signature.getRsaSignature());
                info.setCustomMetadataValue(METADATA_KEY_RSA_SIGNATURE, rsaBase64);
            }

            // Embed PQC/DILITHIUM signature (Base64 encoded)
            if (signature.getPqcSignature() != null && signature.getPqcSignature().length > 0) {
                String pqcBase64 = Base64.getEncoder().encodeToString(signature.getPqcSignature());
                info.setCustomMetadataValue(METADATA_KEY_PQC_SIGNATURE, pqcBase64);
            }

            // Embed sign time
            if (signature.getSignTime() != null) {
                info.setCustomMetadataValue(METADATA_KEY_SIGN_TIME,
                        String.valueOf(signature.getSignTime().getTime()));
            }

            doc.setDocumentInformation(info);

            // Add visible signature annotation
            addVisibleSignatureAnnotation(doc, signature);

            doc.save(pdfPath);
        }

        // Now add proper PDF cryptographic signature if RSA signature exists
        if (signature.getRsaSignature() != null && signature.getRsaSignature().length > 0) {
            addPdfCryptographicSignature(pdfPath, signature);
        }
    }

    /**
     * Adds a proper PDF cryptographic signature (PAdES compatible) to the PDF.
     * This creates a signature that Adobe Reader will recognize as "Signed".
     * Uses PDFBox 2.0.14 API with direct signing approach.
     *
     * @param pdfPath Path to the PDF file
     * @param signature The signature information
     * @throws Exception if signing fails
     */
    private static void addPdfCryptographicSignature(String pdfPath, DocumentSignature signature) throws Exception {
        // Create temporary file for incremental save
        Path tempPath = Files.createTempFile("pdf_sign_", ".pdf");
        Path originalPath = Paths.get(pdfPath);

        try {
            // Load document and prepare for signing
            PDDocument doc = PDDocument.load(new File(pdfPath));

            try {
                // Get the last page for signature appearance
                int pageCount = doc.getNumberOfPages();
                if (pageCount == 0) {
                    throw new Exception("PDF has no pages");
                }
                // Note: Visual signature rectangle not needed for external signing
                // PDFBox will handle signature field creation automatically

                // Create signature dictionary - MUST be created before signing
                PDSignature pdSignature = new PDSignature();

                // Set signature properties
                Calendar cal = Calendar.getInstance();
                if (signature.getSignTime() != null) {
                    cal.setTime(signature.getSignTime());
                }
                pdSignature.setSignDate(cal);

                // Set signature filter and subfilter for PAdES
                pdSignature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
                pdSignature.setSubFilter(PDSignature.SUBFILTER_ETSI_CADES_DETACHED);

                // Set signature name
                String signerName = "PQ-Signatures System";
                if (signature.getScheme() != null) {
                    signerName += " (" + signature.getScheme() + ")";
                }
                pdSignature.setName(signerName);
                pdSignature.setReason("Digital Signature");
                pdSignature.setLocation("Greece");

                // CRITICAL FIX: Do NOT create signature field before signing in PDFBox 2.0.14
                // The saveIncrementalForExternalSigning requires the signature to be added to document first
                // Add signature to document - this tells PDFBox to prepare for signing
                doc.addSignature(pdSignature, new RsaSignatureInterface());

                // Use external signing - this is the correct approach for PDFBox 2.0.14
                try (FileOutputStream output = new FileOutputStream(tempPath.toFile())) {
                    // Create external signing container - this prepares the PDF for signing
                    // It will automatically create signature field and set up byteRange
                    org.apache.pdfbox.pdmodel.interactive.digitalsignature.ExternalSigningSupport externalSigning =
                            doc.saveIncrementalForExternalSigning(output);

                    // Get content to sign (returns InputStream)
                    java.io.InputStream contentToSign = externalSigning.getContent();

                    // Sign the content using our signature interface
                    RsaSignatureInterface signatureInterface = new RsaSignatureInterface();
                    byte[] signatureBytes = signatureInterface.sign(contentToSign);

                    // Set the signature bytes back - this completes the signing and sets byteRange
                    externalSigning.setSignature(signatureBytes);
                } catch (Exception e) {
                    // If external signing fails, log and continue without PDF signature
                    System.err.println("Warning: Could not create PDF cryptographic signature: " + e.getMessage());
                    System.err.println("PDF will have custom metadata signatures but may not show as 'Signed' in Adobe Reader");
                    e.printStackTrace();
                    // Don't throw - allow the process to continue with metadata-only signatures
                    Files.deleteIfExists(tempPath);
                    return; // Exit early, keep original file
                }
            } finally {
                // CRITICAL: Close document BEFORE trying to replace the file
                doc.close();
            }

            // Now that document is closed, replace original file with signed version
            Files.move(tempPath, originalPath, StandardCopyOption.REPLACE_EXISTING);

        } catch (Exception e) {
            // Clean up temp file on error
            try {
                Files.deleteIfExists(tempPath);
            } catch (IOException ignored) {}
            // Don't throw - allow metadata-only signatures to work
            System.err.println("Warning: PDF cryptographic signature failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Adds a visible signature annotation to the PDF document.
     * The annotation appears on the last page of the document.
     *
     * @param doc The PDF document
     * @param signature The signature information
     * @throws IOException if adding annotation fails
     */
    private static void addVisibleSignatureAnnotation(PDDocument doc, DocumentSignature signature) throws IOException {
        int pageCount = doc.getNumberOfPages();
        if (pageCount == 0) {
            return; // No pages to add annotation to
        }

        PDPage page = doc.getPage(pageCount - 1); // Last page
        PDRectangle pageSize = page.getMediaBox();

        // Create signature text annotation
        PDAnnotationText annotation = new PDAnnotationText();
        annotation.setAnnotationName("DigitalSignature");

        // Build signature text
        StringBuilder sigText = new StringBuilder();
        sigText.append("ΨΗΦΙΑΚΗ ΥΠΟΓΡΑΦΗ\n");
        sigText.append("==================\n\n");

        if (signature.getScheme() != null) {
            sigText.append("Σχήμα: ").append(signature.getScheme()).append("\n");
        }

        if (signature.getRsaSignature() != null && signature.getRsaSignature().length > 0) {
            sigText.append("RSA: ✓ Υπογεγραμμένο\n");
        }

        if (signature.getPqcSignature() != null && signature.getPqcSignature().length > 0) {
            sigText.append("DILITHIUM: ✓ Υπογεγραμμένο\n");
        }

        if (signature.getSignTime() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            sigText.append("Ημερομηνία: ").append(sdf.format(signature.getSignTime())).append("\n");
        }

        annotation.setContents(sigText.toString());
        annotation.setSubject("Ψηφιακή Υπογραφή");

        // Position annotation at bottom-right of the page
        float width = 200;
        float height = 100;
        float x = pageSize.getWidth() - width - 20; // 20 points from right edge
        float y = 20; // 20 points from bottom

        PDRectangle rect = new PDRectangle(x, y, width, height);
        annotation.setRectangle(rect);

        // Set appearance (optional, for better visibility)
        annotation.setOpen(false);
        annotation.setColor(new org.apache.pdfbox.pdmodel.graphics.color.PDColor(
                new float[]{0.0f, 0.5f, 0.0f}, // Green color
                org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB.INSTANCE
        ));

        // Add annotation to page
        List<PDAnnotation> annotations = page.getAnnotations();
        if (annotations == null) {
            annotations = new ArrayList<>();
            page.setAnnotations(annotations);
        }
        annotations.add(annotation);

        // Also add a visible signature  field in the form (if form exists or create one)
        try {
            PDAcroForm acroForm = doc.getDocumentCatalog().getAcroForm();
            if (acroForm == null) {
                acroForm = new PDAcroForm(doc);
                doc.getDocumentCatalog().setAcroForm(acroForm);
            }

            // Create signature field
            PDSignatureField sigField = new PDSignatureField(acroForm);
            sigField.setPartialName("DigitalSignature_" + System.currentTimeMillis());
            // Note: Widget methods are deprecated in PDFBox 2.0.14, but still functional
            // For newer versions, use setWidgets() method instead
            @SuppressWarnings("deprecation")
            var widget = sigField.getWidget();
            widget.setRectangle(rect);
            widget.setPage(page);
            widget.setAppearanceState("Signed");

            // Add to form
            acroForm.getFields().add(sigField);
        } catch (Exception e) {
            // If signature field creation fails, continue with text annotation only
            System.err.println("Warning: Could not create signature field: " + e.getMessage());
        }
    }

    /**
     * Embeds a signature into a PDF file using document ID.
     * Reads the file path from FileStorageService.
     *
     * @param documentId The document ID
     * @param signature The signature to embed
     * @throws Exception if embedding fails
     */
    public static void embedSignature(Long documentId, DocumentSignature signature) throws Exception {
        Path filePath = FileStorageService.getFilePath(documentId);
        if (!Files.exists(filePath)) {
            throw new Exception("PDF file not found for document ID: " + documentId);
        }
        embedSignature(filePath.toString(), signature);
    }

    /**
     * Reads embedded signatures from a PDF file.
     *
     * @param pdfPath Path to the PDF file
     * @return DocumentSignature with embedded data, or null if no signatures found
     * @throws Exception if reading fails
     */
    public static DocumentSignature readEmbeddedSignature(String pdfPath) throws Exception {
        try (PDDocument doc = PDDocument.load(new File(pdfPath))) {
            PDDocumentInformation info = doc.getDocumentInformation();

            String scheme = info.getCustomMetadataValue(METADATA_KEY_SCHEME);
            String rsaBase64 = info.getCustomMetadataValue(METADATA_KEY_RSA_SIGNATURE);
            String pqcBase64 = info.getCustomMetadataValue(METADATA_KEY_PQC_SIGNATURE);
            String signTimeStr = info.getCustomMetadataValue(METADATA_KEY_SIGN_TIME);

            // If no signatures found, return null
            if (scheme == null && rsaBase64 == null && pqcBase64 == null) {
                return null;
            }

            DocumentSignature sig = new DocumentSignature();
            sig.setScheme(scheme);

            if (rsaBase64 != null && !rsaBase64.isEmpty()) {
                byte[] rsaBytes = Base64.getDecoder().decode(rsaBase64);
                sig.setRsaSignature(rsaBytes);
            }

            if (pqcBase64 != null && !pqcBase64.isEmpty()) {
                byte[] pqcBytes = Base64.getDecoder().decode(pqcBase64);
                sig.setPqcSignature(pqcBytes);
            }

            if (signTimeStr != null && !signTimeStr.isEmpty()) {
                try {
                    long timestamp = Long.parseLong(signTimeStr);
                    sig.setSignTime(new java.util.Date(timestamp));
                } catch (NumberFormatException e) {
                    // Ignore invalid timestamp
                }
            }

            return sig;
        }
    }

    /**
     * Reads embedded signatures from a PDF file using document ID.
     *
     * @param documentId The document ID
     * @return DocumentSignature with embedded data, or null if no signatures found
     * @throws Exception if reading fails
     */
    public static DocumentSignature readEmbeddedSignature(Long documentId) throws Exception {
        Path filePath = FileStorageService.getFilePath(documentId);
        if (!Files.exists(filePath)) {
            throw new Exception("PDF file not found for document ID: " + documentId);
        }
        return readEmbeddedSignature(filePath.toString());
    }

    /**
     * Checks if a PDF file has embedded signatures.
     *
     * @param pdfPath Path to the PDF file
     * @return true if signatures are embedded, false otherwise
     */
    public static boolean hasEmbeddedSignature(String pdfPath) {
        try {
            DocumentSignature sig = readEmbeddedSignature(pdfPath);
            return sig != null && (sig.getRsaSignature() != null || sig.getPqcSignature() != null);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks if a PDF file has embedded signatures using document ID.
     *
     * @param documentId The document ID
     * @return true if signatures are embedded, false otherwise
     */
    public static boolean hasEmbeddedSignature(Long documentId) {
        try {
            return hasEmbeddedSignature(FileStorageService.getFilePath(documentId).toString());
        } catch (Exception e) {
            return false;
        }
    }
}
