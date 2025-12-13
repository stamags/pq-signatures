package signatures;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

public class ClassicKeyGenBean {

    public static void main(String[] args) throws Exception {
        Path privPath = Path.of("data/rsa-private.key");
        Path pubPath  = Path.of("data/rsa-public.key");

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();

        String privB64 = Base64.getEncoder().encodeToString(kp.getPrivate().getEncoded());
        String pubB64  = Base64.getEncoder().encodeToString(kp.getPublic().getEncoded());

        Files.writeString(privPath, privB64 + "\n", StandardCharsets.UTF_8);
        Files.writeString(pubPath,  pubB64  + "\n", StandardCharsets.UTF_8);

        System.out.println("RSA-2048 keypair generated");
        System.out.println("Private key: " + privPath.toAbsolutePath());
        System.out.println("Public key : " + pubPath.toAbsolutePath());
    }
}
