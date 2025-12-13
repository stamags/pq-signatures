package signatures;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.*;
import java.nio.file.Files;
import java.security.*;
import java.util.Base64;

public class HybridSignPdfBean {


        static {
            Security.addProvider(new BouncyCastleProvider());
        }

        public static void main(String[] args) throws Exception {

            if (args.length != 1) {
                System.out.println("Usage: HybridSignPdf <pdf-file>");
                return;
            }

            File pdf = new File(args[0]);
            byte[] pdfBytes = Files.readAllBytes(pdf.toPath());

            // --- Load RSA keys ---
            PrivateKey rsaPrivate =
                    KeyLoader.loadPrivateKey("data/rsa-private.key", "RSA");

            // --- Load PQC keys (Dilithium) ---
            PrivateKey pqPrivate =
                    KeyLoader.loadPrivateKey("data/pqc-private.key", "DILITHIUM");

            // --- Sign with RSA ---
            Signature rsaSig = Signature.getInstance("SHA256withRSA");
            rsaSig.initSign(rsaPrivate);
            rsaSig.update(pdfBytes);
            byte[] rsaSignature = rsaSig.sign();

            // --- Sign with Dilithium ---
            Signature pqSig = Signature.getInstance("DILITHIUM");
            pqSig.initSign(pqPrivate);
            pqSig.update(pdfBytes);
            byte[] pqSignature = pqSig.sign();

            // --- Write signatures ---
            try (PrintWriter out = new PrintWriter("data/pdf-signatures.txt")) {
                out.println("RSA=" + Base64.getEncoder().encodeToString(rsaSignature));
                out.println("PQC=" + Base64.getEncoder().encodeToString(pqSignature));
            }

            System.out.println("PDF signed successfully (RSA + PQC)");
        }


}
