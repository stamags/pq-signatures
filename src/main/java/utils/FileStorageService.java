package utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Service για αποθήκευση και ανάκτηση αρχείων από το filesystem.
 * Τα αρχεία αποθηκεύονται σε: {storage.base}/uploads/{documentId}.pdf
 */
public class FileStorageService {

    private static final String DEFAULT_STORAGE_BASE = System.getProperty("user.home") + "/pq-signatures-uploads";
    private static final String UPLOADS_SUBDIR = "uploads";

    /**
     * Λήψη του βασικού καταλόγου αποθήκευσης.
     * Μπορεί να ρυθμιστεί μέσω system property: pq.signatures.storage.base
     */
    public static String getStorageBase() {
        String base = System.getProperty("pq.signatures.storage.base");
        if (base == null || base.isEmpty()) {
            base = DEFAULT_STORAGE_BASE;
        }
        return base;
    }

    /**
     * Λήψη της διαδρομής του καταλόγου uploads.
     */
    public static Path getUploadsDirectory() {
        return Paths.get(getStorageBase(), UPLOADS_SUBDIR);
    }

    /**
     * Λήψη της διαδρομής αρχείου για ένα document ID.
     */
    public static Path getFilePath(Long documentId) {
        return getUploadsDirectory().resolve(documentId + ".pdf");
    }

    /**
     * Αποθήκευση αρχείου στο filesystem και επιστροφή της διαδρομής αποθήκευσης.
     */
    public static String saveFile(Long documentId, byte[] fileData) throws IOException {
        Path uploadsDir = getUploadsDirectory();
        Files.createDirectories(uploadsDir);

        Path filePath = getFilePath(documentId);
        Files.write(filePath, fileData);

        return filePath.toString();
    }

    /**
     * Ανάγνωση αρχείου από το filesystem.
     */
    public static byte[] readFile(Long documentId) throws IOException {
        Path filePath = getFilePath(documentId);
        if (!Files.exists(filePath)) {
            throw new IOException("Το αρχείο δεν βρέθηκε για document ID: " + documentId);
        }
        return Files.readAllBytes(filePath);
    }


    /**
     * Υπολογισμός SHA-256 hash των δεδομένων αρχείου.
     * Calculates the SHA-256 hash of the given data and returns it
     * και επειδή χρησημοποιούμε java21 χρησιμοποιούμε την μέθοδο HexFormat.of()
     */
    public static String calculateSha256(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");

        byte[] hash = sha256.digest(data);

        return HexFormat.of().formatHex(hash);
    }
}


