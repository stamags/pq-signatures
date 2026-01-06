package signatures;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class KeyLoader {

    static {
        // Ensure BouncyCastle provider is available
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /**
     * Get the base directory for key files.
     * Priority:
     * 1. System property: pq.signatures.keys.dir
     * 2. Relative to user.dir (project root): ./data
     * 3. Relative to user.home: ~/pq-signatures-keys/data
     */
    private static Path getKeysBaseDir() {
        // Check system property first
        String keysDir = System.getProperty("pq.signatures.keys.dir");
        if (keysDir != null && !keysDir.isEmpty()) {
            return Paths.get(keysDir);
        }

        // Try relative to current working directory (project root)
        Path relativePath = Paths.get("data");
        if (Files.exists(relativePath)) {
            return relativePath.toAbsolutePath();
        }

        // Fallback to user home
        return Paths.get(System.getProperty("user.home"), "pq-signatures-keys", "data");
    }

    /**
     * Resolve key file path.
     */
    private static Path resolveKeyPath(String relativePath) {
        Path baseDir = getKeysBaseDir();
        Path keyPath = baseDir.resolve(relativePath);
        
        // If relative path already contains "data/", remove it
        if (relativePath.startsWith("data/")) {
            keyPath = baseDir.resolve(relativePath.substring(5)); // Remove "data/" prefix
        }
        
        return keyPath;
    }

    public static PrivateKey loadPrivateKey(String path, String algorithm) throws Exception {
        Path keyPath = resolveKeyPath(path);
        byte[] der = readKeyBytes(keyPath); // <-- Base64 -> DER
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);

        // For DILITHIUM/DILITHIUM3, use BC provider
        KeyFactory kf;
        if ("DILITHIUM".equalsIgnoreCase(algorithm) || "DILITHIUM3".equalsIgnoreCase(algorithm)) {
            kf = KeyFactory.getInstance("DILITHIUM", "BC");
        } else {
            kf = KeyFactory.getInstance(algorithm);
        }
        return kf.generatePrivate(spec);
    }

    public static PublicKey loadPublicKey(String path, String algorithm) throws Exception {
        Path keyPath = resolveKeyPath(path);
        byte[] der = readKeyBytes(keyPath); // <-- Base64 -> DER
        X509EncodedKeySpec spec = new X509EncodedKeySpec(der);

        // For DILITHIUM/DILITHIUM3, use BC provider
        KeyFactory kf;
        if ("DILITHIUM".equalsIgnoreCase(algorithm) || "DILITHIUM3".equalsIgnoreCase(algorithm)) {
            kf = KeyFactory.getInstance("DILITHIUM", "BC");
        } else {
            kf = KeyFactory.getInstance(algorithm);
        }
        return kf.generatePublic(spec);
    }

    private static byte[] readKeyBytes(Path keyPath) throws Exception {
        if (!Files.exists(keyPath)) {
            throw new Exception("Key file not found: " + keyPath.toAbsolutePath() + 
                "\nPlease generate keys first using:\n" +
                "  - ClassicKeyGenBean (for RSA keys)\n" +
                "  - PqcKeyGenBean (for PQC/DILITHIUM keys)\n" +
                "Or set system property: -Dpq.signatures.keys.dir=/path/to/keys");
        }
        
        // Διαβάζουμε σαν κείμενο, πετάμε whitespace/newlines, κάνουμε Base64 decode
        String s = Files.readString(keyPath, StandardCharsets.UTF_8).trim();

        // Αν τυχόν έχει “PEM-like” headers, τα αφαιρούμε
        s = s.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", ""); // remove ALL whitespace

        return Base64.getDecoder().decode(s);
    }
}
