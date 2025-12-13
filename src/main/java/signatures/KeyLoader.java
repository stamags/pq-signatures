package signatures;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class KeyLoader {

    public static PrivateKey loadPrivateKey(String path, String algorithm) throws Exception {
        byte[] der = readKeyBytes(path); // <-- Base64 -> DER
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);

        // Για RSA δεν χρειάζεται BC provider, αλλά δεν πειράζει
        KeyFactory kf = KeyFactory.getInstance(algorithm);
        return kf.generatePrivate(spec);
    }

    public static PublicKey loadPublicKey(String path, String algorithm) throws Exception {
        byte[] der = readKeyBytes(path); // <-- Base64 -> DER
        X509EncodedKeySpec spec = new X509EncodedKeySpec(der);

        KeyFactory kf = KeyFactory.getInstance(algorithm);
        return kf.generatePublic(spec);
    }

    private static byte[] readKeyBytes(String path) throws Exception {
        // Διαβάζουμε σαν κείμενο, πετάμε whitespace/newlines, κάνουμε Base64 decode
        String s = Files.readString(Path.of(path), StandardCharsets.UTF_8).trim();

        // Αν τυχόν έχει “PEM-like” headers, τα αφαιρούμε
        s = s.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", ""); // remove ALL whitespace

        return Base64.getDecoder().decode(s);
    }
}
