package signatures;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;


public class PqcSignPdfBean {

    private static final String ALGO = "DILITHIUM3";
    private static final String PROVIDER = "BC";

    // Usage: PqcSignPdf data/sample.pdf
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("Usage: PqcSignPdf <path-to-pdf>");
            System.out.println("Example: PqcSignPdf data/sample.pdf");
            return;
        }

        ensureBC();

        Path pdfPath = Path.of(args[0]);
        if (!Files.exists(pdfPath) || !pdfPath.toString().toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Input must be an existing .pdf file: " + pdfPath);
        }

        Path privKeyPath = Path.of("data/private.key");
        Path pubKeyPath  = Path.of("data/public.key");

        Path sigPath  = Path.of(args[0] + ".sig");
        Path metaPath = Path.of(args[0] + ".meta.json");

        byte[] pdfBytes = Files.readAllBytes(pdfPath);

        // Hash PDF bytes
        String sha256Hex = sha256Hex(pdfBytes);

        // Load private key
        String privB64 = Files.readString(privKeyPath, StandardCharsets.UTF_8).trim();
        byte[] privBytes = Base64.getDecoder().decode(privB64);

        KeyFactory kf = KeyFactory.getInstance(ALGO, PROVIDER);
        PrivateKey priv = kf.generatePrivate(new PKCS8EncodedKeySpec(privBytes));

        // Sign
        Signature s = Signature.getInstance(ALGO, PROVIDER);
        s.initSign(priv);
        s.update(pdfBytes);
        byte[] signature = s.sign();

        String sigB64 = Base64.getEncoder().encodeToString(signature);
        Files.writeString(sigPath, sigB64 + "\n", StandardCharsets.UTF_8);

        // Read public key (Base64)
        String pubB64 = Files.readString(pubKeyPath, StandardCharsets.UTF_8).trim();

        Provider p = Security.getProvider(PROVIDER);
        String providerVersion = (p != null) ? p.getVersionStr() : "unknown";

        // meta json
        String metaJson = "{\n" +
                "  \"type\": \"PDF_DETACHED_SIGNATURE\",\n" +
                "  \"algorithm\": \"" + ALGO + "\",\n" +
                "  \"provider\": \"" + PROVIDER + "\",\n" +
                "  \"providerVersion\": \"" + escape(providerVersion) + "\",\n" +
                "  \"createdAt\": \"" + Instant.now().toString() + "\",\n" +
                "  \"pdfPath\": \"" + escape(pdfPath.toString()) + "\",\n" +
                "  \"pdfSha256\": \"" + sha256Hex + "\",\n" +
                "  \"publicKeyBase64\": \"" + pubB64 + "\",\n" +
                "  \"signatureBase64\": \"" + sigB64 + "\"\n" +
                "}\n";

        Files.writeString(metaPath, metaJson, StandardCharsets.UTF_8);

        System.out.println("Signed PDF : " + pdfPath.toAbsolutePath());
        System.out.println("SHA-256    : " + sha256Hex);
        System.out.println("Sig bytes  : " + signature.length);
        System.out.println("Wrote      : " + sigPath.toAbsolutePath());
        System.out.println("Wrote      : " + metaPath.toAbsolutePath());
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
