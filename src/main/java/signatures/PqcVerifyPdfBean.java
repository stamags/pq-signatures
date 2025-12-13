package beans;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;


public class PqcVerifyPdfBean {

    private static final String PROVIDER = "BC";

    // Usage: PqcVerifyPdf data/sample.pdf
    // expects: data/sample.pdf.meta.json
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("Usage: PqcVerifyPdf <path-to-pdf>");
            System.out.println("Example: PqcVerifyPdf data/sample.pdf");
            return;
        }

        ensureBC();

        Path pdfPath = Path.of(args[0]);
        Path metaPath = Path.of(args[0] + ".meta.json");

        String meta = Files.readString(metaPath, StandardCharsets.UTF_8);

        String algorithm = readJsonString(meta, "algorithm");
        String expectedSha256 = readJsonString(meta, "pdfSha256");
        String pubB64 = readJsonString(meta, "publicKeyBase64");
        String sigB64 = readJsonString(meta, "signatureBase64");

        byte[] pdfBytes = Files.readAllBytes(pdfPath);
        String actualSha256 = sha256Hex(pdfBytes);
        boolean hashOk = actualSha256.equalsIgnoreCase(expectedSha256);

        byte[] pubBytes = Base64.getDecoder().decode(pubB64);
        byte[] signature = Base64.getDecoder().decode(sigB64);

        KeyFactory kf = KeyFactory.getInstance(algorithm, PROVIDER);
        PublicKey pub = kf.generatePublic(new X509EncodedKeySpec(pubBytes));

        Signature v = Signature.getInstance(algorithm, PROVIDER);
        v.initVerify(pub);
        v.update(pdfBytes);
        boolean sigOk = v.verify(signature);

        System.out.println("Verify PDF  : " + pdfPath.toAbsolutePath());
        System.out.println("Meta        : " + metaPath.toAbsolutePath());
        System.out.println("Algorithm   : " + algorithm);
        System.out.println("SHA meta    : " + expectedSha256);
        System.out.println("SHA file    : " + actualSha256);
        System.out.println("Hash match? : " + (hashOk ? "YES ✅" : "NO ❌"));
        System.out.println("Sig verify? : " + (sigOk ? "OK ✅" : "FAIL ❌"));
        System.out.println("Result      : " + ((hashOk && sigOk) ? "OK ✅" : "FAIL ❌"));
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

    private static String readJsonString(String json, String key) {
        String pattern = "\"" + key + "\"";
        int i = json.indexOf(pattern);
        if (i < 0) throw new IllegalArgumentException("Missing key: " + key);
        int colon = json.indexOf(':', i);
        int firstQuote = json.indexOf('"', colon + 1);
        int secondQuote = json.indexOf('"', firstQuote + 1);
        return json.substring(firstQuote + 1, secondQuote);
    }
}
