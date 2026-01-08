package signatures;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.util.Base64;

/**
 * Utility to generate both RSA and PQC keys.
 * Run this once to create all required key files.
 */
public class GenerateKeys {

    public static void main(String[] args) throws Exception {
        // Ensure BouncyCastle is available
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }

        // Determine output directory
        Path outDir = Paths.get("data");
        Files.createDirectories(outDir);

        System.out.println("Generating keys in: " + outDir.toAbsolutePath());
        System.out.println();

        // 1. Generate RSA keys
        System.out.println("Generating RSA-2048 keys...");
        KeyPairGenerator rsaKpg = KeyPairGenerator.getInstance("RSA");
        rsaKpg.initialize(2048);
        KeyPair rsaKp = rsaKpg.generateKeyPair();

        String rsaPrivB64 = Base64.getEncoder().encodeToString(rsaKp.getPrivate().getEncoded());
        String rsaPubB64 = Base64.getEncoder().encodeToString(rsaKp.getPublic().getEncoded());

        Files.writeString(outDir.resolve("rsa-private.key"), rsaPrivB64 + "\n", StandardCharsets.UTF_8);
        Files.writeString(outDir.resolve("rsa-public.key"), rsaPubB64 + "\n", StandardCharsets.UTF_8);

        System.out.println("✓ RSA keys generated:");
        System.out.println("  - " + outDir.resolve("rsa-private.key").toAbsolutePath());
        System.out.println("  - " + outDir.resolve("rsa-public.key").toAbsolutePath());
        System.out.println();

        // 2. Generate PQC (DILITHIUM) keys
        System.out.println("Generating DILITHIUM3 keys...");
        KeyPairGenerator pqcKpg = KeyPairGenerator.getInstance("DILITHIUM3", "BC");
        KeyPair pqcKp = pqcKpg.generateKeyPair();

        String pqcPrivB64 = Base64.getEncoder().encodeToString(pqcKp.getPrivate().getEncoded());
        String pqcPubB64 = Base64.getEncoder().encodeToString(pqcKp.getPublic().getEncoded());

        Files.writeString(outDir.resolve("pqc-private.key"), pqcPrivB64 + "\n", StandardCharsets.UTF_8);
        Files.writeString(outDir.resolve("pqc-public.key"), pqcPubB64 + "\n", StandardCharsets.UTF_8);

        System.out.println("✓ PQC keys generated:");
        System.out.println("  - " + outDir.resolve("pqc-private.key").toAbsolutePath());
        System.out.println("  - " + outDir.resolve("pqc-public.key").toAbsolutePath());
        System.out.println();

        System.out.println("All keys generated successfully!");
    }
}


