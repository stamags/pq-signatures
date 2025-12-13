package signatures;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import java.security.MessageDigest;

public class PqcVerifyFileBean {

    // Usage: run with args: data/input.json
    // expects:
    // - data/public.key
    // - data/input.json.sig
//    public static void main(String[] args) throws Exception {
//        if (args.length != 1) {
//            System.out.println("Usage: PqcVerifyFile <path-to-input-file>");
//            System.out.println("Example: PqcVerifyFile data/input.json");
//            return;
//        }
//
//        if (Security.getProvider("BC") == null) {
//            Security.addProvider(new BouncyCastleProvider());
//        }
//
//        Path input = Path.of(args[0]);
//        Path pubKeyPath = Path.of("data/public.key");
//        Path sigPath = Path.of(args[0] + ".sig");
//
//        byte[] data = Files.readAllBytes(input);
//
//        String pubB64 = Files.readString(pubKeyPath, StandardCharsets.UTF_8).trim();
//        byte[] pubBytes = Base64.getDecoder().decode(pubB64);
//
//        String sigB64 = Files.readString(sigPath, StandardCharsets.UTF_8).trim();
//        byte[] signature = Base64.getDecoder().decode(sigB64);
//
//        KeyFactory kf = KeyFactory.getInstance("DILITHIUM3", "BC");
//        PublicKey pub = kf.generatePublic(new X509EncodedKeySpec(pubBytes));
//
//        Signature v = Signature.getInstance("DILITHIUM3", "BC");
//        v.initVerify(pub);
//        v.update(data);
//
//        boolean ok = v.verify(signature);
//
//        System.out.println("Verify input : " + input.toAbsolutePath());
//        System.out.println("Using pubkey : " + pubKeyPath.toAbsolutePath());
//        System.out.println("Using sig    : " + sigPath.toAbsolutePath());
//        System.out.println("Result       : " + (ok ? "OK ✅" : "FAIL ❌"));
//    }
    private static final String PROVIDER = "BC";

    // Usage: PqcVerifyFile data/input.json
    // expects: data/input.json.meta.json
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("Usage: PqcVerifyFile <path-to-input-file>");
            System.out.println("Example: PqcVerifyFile data/input.json");
            return;
        }

        ensureBC();

        Path input = Path.of(args[0]);
        Path metaPath = Path.of(args[0] + ".meta.json");

        String meta = Files.readString(metaPath, StandardCharsets.UTF_8);

        String algorithm = readJsonString(meta, "algorithm");
        String expectedSha256 = readJsonString(meta, "fileSha256");
        String pubB64 = readJsonString(meta, "publicKeyBase64");
        String sigB64 = readJsonString(meta, "signatureBase64");

        byte[] data = Files.readAllBytes(input);
        String actualSha256 = sha256Hex(data);

        boolean hashOk = actualSha256.equalsIgnoreCase(expectedSha256);

        byte[] pubBytes = Base64.getDecoder().decode(pubB64);
        byte[] signature = Base64.getDecoder().decode(sigB64);

        KeyFactory kf = KeyFactory.getInstance(algorithm, PROVIDER);
        PublicKey pub = kf.generatePublic(new X509EncodedKeySpec(pubBytes));

        Signature v = Signature.getInstance(algorithm, PROVIDER);
        v.initVerify(pub);
        v.update(data);

        boolean sigOk = v.verify(signature);

        System.out.println("Verify input : " + input.toAbsolutePath());
        System.out.println("Meta         : " + metaPath.toAbsolutePath());
        System.out.println("Algorithm    : " + algorithm);
        System.out.println("SHA-256 meta : " + expectedSha256);
        System.out.println("SHA-256 file : " + actualSha256);
        System.out.println("Hash match?  : " + (hashOk ? "YES ✅" : "NO ❌"));
        System.out.println("Sig verify?  : " + (sigOk ? "OK ✅" : "FAIL ❌"));
        System.out.println("Result       : " + ((hashOk && sigOk) ? "OK ✅" : "FAIL ❌"));
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

    // Πολύ απλό “parser” για το συγκεκριμένο meta.json που γράφουμε.
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
