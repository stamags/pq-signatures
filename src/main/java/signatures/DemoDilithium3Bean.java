package signatures;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.nio.charset.StandardCharsets;
import java.security.*;


public class DemoDilithium3Bean {
    public static void main(String[] args) throws Exception {
        // 1) Register BC provider (BC 1.7801 όπως είδες)
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }

        // 2) Generate Dilithium3 key pair
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("DILITHIUM3", "BC");
        KeyPair kp = kpg.generateKeyPair();

        byte[] message = "Hello PQC! This is a Dilithium3 signature demo."
                .getBytes(StandardCharsets.UTF_8);

        // 3) Sign
        Signature signer = Signature.getInstance("DILITHIUM3", "BC");
        signer.initSign(kp.getPrivate());
        signer.update(message);
        byte[] signature = signer.sign();

        // 4) Verify
        Signature verifier = Signature.getInstance("DILITHIUM3", "BC");
        verifier.initVerify(kp.getPublic());
        verifier.update(message);
        boolean ok = verifier.verify(signature);

        System.out.println("Algorithm: DILITHIUM3 (PQC)");
        System.out.println("Public key bytes : " + kp.getPublic().getEncoded().length);
        System.out.println("Private key bytes: " + kp.getPrivate().getEncoded().length);
        System.out.println("Signature bytes  : " + signature.length);
        System.out.println("Verify OK?       : " + ok);

        // 5) Negative test (change message)
        byte[] tampered = "Hello PQC! This is a Tampered message."
                .getBytes(StandardCharsets.UTF_8);

        verifier = Signature.getInstance("DILITHIUM3", "BC");
        verifier.initVerify(kp.getPublic());
        verifier.update(tampered);
        boolean ok2 = verifier.verify(signature);

        System.out.println("Verify after tamper OK? : " + ok2);
    }
}
