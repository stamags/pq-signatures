package signatures;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Security;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.security.MessageDigest;
import java.security.Provider;
import java.time.Instant;

public class PqcSignFileBean {

//    // Usage: run with args: data/input.json  (ή data/input.pdf)
//    public static void main(String[] args) throws Exception {
//        if (args.length != 1) {
//            System.out.println("Usage: PqcSignFile <path-to-input-file>");
//            System.out.println("Example: PqcSignFile data/input.json");
//            return;
//        }
//
//        if (Security.getProvider("BC") == null) {
//            Security.addProvider(new BouncyCastleProvider());
//        }
//
//        Path input = Path.of(args[0]);
//        Path privKeyPath = Path.of("data/private.key");
//        Path sigPath = Path.of(args[0] + ".sig");
//
//        byte[] data = Files.readAllBytes(input);
//
//        String privB64 = Files.readString(privKeyPath, StandardCharsets.UTF_8).trim();
//        byte[] privBytes = Base64.getDecoder().decode(privB64);
//
//        KeyFactory kf = KeyFactory.getInstance("DILITHIUM3", "BC");
//        PrivateKey priv = kf.generatePrivate(new PKCS8EncodedKeySpec(privBytes));
//
//        Signature s = Signature.getInstance("DILITHIUM3", "BC");
//        s.initSign(priv);
//        s.update(data);
//        byte[] signature = s.sign();
//
//        Files.writeString(sigPath, Base64.getEncoder().encodeToString(signature) + "\n", StandardCharsets.UTF_8);
//
//        System.out.println("Signed: " + input.toAbsolutePath());
//        System.out.println("Signature (Base64) saved to: " + sigPath.toAbsolutePath());
//        System.out.println("Signature bytes: " + signature.length);
//    }

    private static final String ALGO = "DILITHIUM3";
    private static final String PROVIDER = "BC";

    // Usage: PqcSignFile data/input.json
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("Usage: PqcSignFile <path-to-input-file>");
            System.out.println("Example: PqcSignFile data/input.json");
            return;
        }

        ensureBC();

        Path input = Path.of(args[0]);
        Path privKeyPath = Path.of("data/private.key");
        Path sigPath = Path.of(args[0] + ".sig");
        Path metaPath = Path.of(args[0] + ".meta.json");

        byte[] data = Files.readAllBytes(input);

        // SHA-256 of file
        String sha256Hex = sha256Hex(data);

        // Load private key (Base64 PKCS8)
        String privB64 = Files.readString(privKeyPath, StandardCharsets.UTF_8).trim();
        byte[] privBytes = Base64.getDecoder().decode(privB64);

        KeyFactory kf = KeyFactory.getInstance(ALGO, PROVIDER);
        PrivateKey priv = kf.generatePrivate(new PKCS8EncodedKeySpec(privBytes));

        // Sign
        Signature s = Signature.getInstance(ALGO, PROVIDER);
        s.initSign(priv);
        s.update(data);
        byte[] signature = s.sign();

        String sigB64 = Base64.getEncoder().encodeToString(signature);

        // Save .sig (Base64)
        Files.writeString(sigPath, sigB64 + "\n", StandardCharsets.UTF_8);

        // For meta: include public key as Base64 too (derivable from private? όχι πάντα με JCA)
        // Άρα: διαβάζουμε το public.key από data/public.key
        Path pubKeyPath = Path.of("data/public.key");
        String pubB64 = Files.readString(pubKeyPath, StandardCharsets.UTF_8).trim();

        Provider p = Security.getProvider(PROVIDER);
        String providerVersion = (p != null) ? p.getVersionStr() : "unknown";

        // Build meta JSON (χειροκίνητα για να μη βάλουμε extra dependencies)
        String metaJson = "{\n" +
                "  \"algorithm\": \"" + ALGO + "\",\n" +
                "  \"provider\": \"" + PROVIDER + "\",\n" +
                "  \"providerVersion\": \"" + escape(providerVersion) + "\",\n" +
                "  \"createdAt\": \"" + Instant.now().toString() + "\",\n" +
                "  \"filePath\": \"" + escape(input.toString()) + "\",\n" +
                "  \"fileSha256\": \"" + sha256Hex + "\",\n" +
                "  \"publicKeyBase64\": \"" + pubB64 + "\",\n" +
                "  \"signatureBase64\": \"" + sigB64 + "\"\n" +
                "}\n";

        Files.writeString(metaPath, metaJson, StandardCharsets.UTF_8);

        System.out.println("Signed: " + input.toAbsolutePath());
        System.out.println("SHA-256: " + sha256Hex);
        System.out.println("Signature bytes: " + signature.length);
        System.out.println("Wrote: " + sigPath.toAbsolutePath());
        System.out.println("Wrote: " + metaPath.toAbsolutePath());
    }

    private static void ensureBC() {
        if (Security.getProvider(PROVIDER) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private static String sha256Hex(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(data);
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for (byte b : digest) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
