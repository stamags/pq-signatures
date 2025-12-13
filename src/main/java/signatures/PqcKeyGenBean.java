package beans;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.util.Base64;

public class PqcKeyGenBean {

    public static void main(String[] args) throws Exception {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }

        Path outDir = Path.of("data");
        Files.createDirectories(outDir);

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("DILITHIUM3", "BC");
        KeyPair kp = kpg.generateKeyPair();

        // Store in Base64 (DER encoded key bytes)
        String pubB64 = Base64.getEncoder().encodeToString(kp.getPublic().getEncoded());
        String privB64 = Base64.getEncoder().encodeToString(kp.getPrivate().getEncoded());

        Files.writeString(outDir.resolve("public.key"), pubB64 + "\n", StandardCharsets.UTF_8);
        Files.writeString(outDir.resolve("private.key"), privB64 + "\n", StandardCharsets.UTF_8);

        System.out.println("Wrote keys to:");
        System.out.println(" - " + outDir.resolve("public.key").toAbsolutePath());
        System.out.println(" - " + outDir.resolve("private.key").toAbsolutePath());
        System.out.println("Public bytes : " + kp.getPublic().getEncoded().length);
        System.out.println("Private bytes: " + kp.getPrivate().getEncoded().length);
    }
}
