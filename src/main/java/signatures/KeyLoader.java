package signatures;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import utils.UserKeystoreService;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class KeyLoader {

    static {
        // Εξασφάλιση ότι ο BouncyCastle provider είναι διαθέσιμος
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /**
     * Λήψη του βασικού καταλόγου για αρχεία κλειδιών.
     * Προτεραιότητα:
     * 1. System property: pq.signatures.keys.dir
     * 2. Σχετικό με user.dir (project root): ./data
     * 3. Σχετικό με user.home: ~/pq-signatures-keys/data
     */
    private static Path getKeysBaseDir() {
        // Έλεγχος system property πρώτα
        String keysDir = System.getProperty("pq.signatures.keys.dir");
        if (keysDir != null && !keysDir.isEmpty()) {
            return Paths.get(keysDir);
        }

        // Δοκιμή σχετικά με τον τρέχοντα working directory (project root)
        Path relativePath = Paths.get("data");
        if (Files.exists(relativePath)) {
            return relativePath.toAbsolutePath();
        }

        // Fallback στο user home
        return Paths.get(System.getProperty("user.home"), "pq-signatures-keys", "data");
    }

    /**
     * Επίλυση διαδρομής αρχείου κλειδιού.
     */
    private static Path resolveKeyPath(String relativePath) {
        Path baseDir = getKeysBaseDir();
        Path keyPath = baseDir.resolve(relativePath);

        // Αν η σχετική διαδρομή περιέχει ήδη "data/", την αφαιρούμε
        if (relativePath.startsWith("data/")) {
            keyPath = baseDir.resolve(relativePath.substring(5)); // Αφαίρεση "data/" prefix
        }

        return keyPath;
    }

    public static PrivateKey loadPrivateKey(String path, String algorithm) throws Exception {
        Path keyPath = resolveKeyPath(path);
        byte[] der = readKeyBytes(keyPath); // <-- Base64 -> DER
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);

        // Για DILITHIUM/DILITHIUM3, χρήση BC provider
        KeyFactory kf;
        if ("DILITHIUM".equalsIgnoreCase(algorithm) || "DILITHIUM3".equalsIgnoreCase(algorithm)) {
            kf = KeyFactory.getInstance("DILITHIUM3", "BC");
        } else {
            kf = KeyFactory.getInstance(algorithm);
        }
        return kf.generatePrivate(spec);
    }

    public static PublicKey loadPublicKey(String path, String algorithm) throws Exception {
        Path keyPath = resolveKeyPath(path);
        byte[] der = readKeyBytes(keyPath); // <-- Base64 -> DER
        return publicKeyFromDer(der, algorithm);
    }

    // ---------- Φόρτωση κλειδιών ανά χρήστη (από UserKeystoreService) ----------

    /**
     * Φόρτωση ιδιωτικού κλειδιού RSA από το keystore του χρήστη.
     */
    public static PrivateKey loadRsaPrivateKeyForUser(String username, char[] keystorePassword) throws Exception {
        Path keystorePath = UserKeystoreService.getKeystorePath(username);
        if (!Files.exists(keystorePath)) {
            throw new Exception("User keystore not found for: " + username + " at " + keystorePath.toAbsolutePath());
        }
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (InputStream is = Files.newInputStream(keystorePath)) {
            ks.load(is, keystorePassword);
        }
        KeyStore.ProtectionParameter protParam = new KeyStore.PasswordProtection(keystorePassword);
        KeyStore.PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry) ks.getEntry(UserKeystoreService.RSA_ALIAS, protParam);
        if (entry == null) {
            throw new Exception("RSA key entry not found in keystore for: " + username);
        }
        return entry.getPrivateKey();
    }

    /**
     * Φόρτωση δημόσιου κλειδιού RSA του χρήστη (από αρχείο, χωρίς κωδικό).
     */
    public static PublicKey loadRsaPublicKeyForUser(String username) throws Exception {
        Path path = UserKeystoreService.getRsaPublicKeyPath(username);
        byte[] der = readKeyBytesFromPath(path);
        return publicKeyFromDer(der, "RSA");
    }

    /**
     * Φόρτωση ιδιωτικού κλειδιού PQC (Dilithium) του χρήστη.
     */
    public static PrivateKey loadPqcPrivateKeyForUser(String username) throws Exception {
        Path path = UserKeystoreService.getPqcPrivateKeyPath(username);
        byte[] der = readKeyBytesFromPath(path);
        KeyFactory kf = KeyFactory.getInstance("DILITHIUM3", "BC");
        return kf.generatePrivate(new PKCS8EncodedKeySpec(der));
    }

    /**
     * Φόρτωση δημόσιου κλειδιού PQC (Dilithium) του χρήστη.
     */
    public static PublicKey loadPqcPublicKeyForUser(String username) throws Exception {
        Path path = UserKeystoreService.getPqcPublicKeyPath(username);
        byte[] der = readKeyBytesFromPath(path);
        return publicKeyFromDer(der, "DILITHIUM");
    }

    /**
     * Δημιουργία PublicKey από DER bytes και αλγόριθμο.
     */
    private static PublicKey publicKeyFromDer(byte[] der, String algorithm) throws Exception {
        X509EncodedKeySpec spec = new X509EncodedKeySpec(der);
        KeyFactory kf;
        if ("DILITHIUM".equalsIgnoreCase(algorithm) || "DILITHIUM3".equalsIgnoreCase(algorithm)) {
            kf = KeyFactory.getInstance("DILITHIUM3", "BC");
        } else {
            kf = KeyFactory.getInstance(algorithm);
        }
        return kf.generatePublic(spec);
    }

    /**
     * Ανάγνωση Base64-encoded κλειδιού από Path (ίδια λογική με readKeyBytes).
     */
    private static byte[] readKeyBytesFromPath(Path keyPath) throws Exception {
        if (!Files.exists(keyPath)) {
            throw new Exception("Key file not found: " + keyPath.toAbsolutePath());
        }
        String s = Files.readString(keyPath, StandardCharsets.UTF_8).trim();
        s = s.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        return Base64.getDecoder().decode(s);
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
                .replaceAll("\\s+", ""); // αφαίρεση ΟΛΟΥ του whitespace

        return Base64.getDecoder().decode(s);
    }
}
