package utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Service for storing and retrieving files from filesystem.
 * Files are stored in: {storage.base}/uploads/{documentId}.pdf
 */
public class FileStorageService {

    private static final String DEFAULT_STORAGE_BASE = System.getProperty("user.home") + "/pq-signatures-uploads";
    private static final String UPLOADS_SUBDIR = "uploads";

    /**
     * Get the base storage directory.
     * Can be configured via system property: pq.signatures.storage.base
     */
    public static String getStorageBase() {
        String base = System.getProperty("pq.signatures.storage.base");
        if (base == null || base.isEmpty()) {
            base = DEFAULT_STORAGE_BASE;
        }
        return base;
    }

    /**
     * Get the uploads directory path.
     */
    public static Path getUploadsDirectory() {
        return Paths.get(getStorageBase(), UPLOADS_SUBDIR);
    }

    /**
     * Get the file path for a document ID.
     */
    public static Path getFilePath(Long documentId) {
        return getUploadsDirectory().resolve(documentId + ".pdf");
    }

    /**
     * Save file to filesystem and return the storage path.
     */
    public static String saveFile(Long documentId, byte[] fileData) throws IOException {
        Path uploadsDir = getUploadsDirectory();
        Files.createDirectories(uploadsDir);

        Path filePath = getFilePath(documentId);
        Files.write(filePath, fileData);

        return filePath.toString();
    }

    /**
     * Read file from filesystem.
     */
    public static byte[] readFile(Long documentId) throws IOException {
        Path filePath = getFilePath(documentId);
        if (!Files.exists(filePath)) {
            throw new IOException("File not found for document ID: " + documentId);
        }
        return Files.readAllBytes(filePath);
    }

    /**
     * Delete file from filesystem.
     */
    public static boolean deleteFile(Long documentId) {
        try {
            Path filePath = getFilePath(documentId);
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Check if file exists.
     */
    public static boolean fileExists(Long documentId) {
        Path filePath = getFilePath(documentId);
        return Files.exists(filePath);
    }

    /**
     * Calculate SHA-256 hash of file data.
     */
    public static String calculateSha256(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(data);
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}

