package signatures;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.*;
import java.nio.file.Files;
import java.security.*;
import java.util.Base64;
import java.util.List;

public class HybridVerifyPdfBean {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static void main(String[] args) throws Exception {

        if (args.length != 1) {
            System.out.println("Usage: HybridVerifyPdf <pdf-file>");
            return;
        }

        File pdf = new File(args[0]);
//        byte[] pdfBytes = Files.readAllBytes(pdf.toPath());
        byte[] toBeVerified = utils.PdfCanonicalUtil.canonicalBytesWithoutSignatures(pdf.getPath());


        PublicKey rsaPublic =
                KeyLoader.loadPublicKey("data/rsa-public.key", "RSA");

        PublicKey pqPublic =
                KeyLoader.loadPublicKey("data/pqc-public.key", "DILITHIUM");

       /* List<String> lines = Files.readAllLines(
                new File("data/pdf-signatures.txt").toPath());

        byte[] rsaSigBytes = Base64.getDecoder()
                .decode(lines.get(0).split("=")[1]);

        byte[] pqSigBytes = Base64.getDecoder()
                .decode(lines.get(1).split("=")[1]);*/

        String rsaB64 = utils.PdfMetadataUtil.readMetadata(pdf.getPath(), "RSA_SIGNATURE");
        String pqcB64 = utils.PdfMetadataUtil.readMetadata(pdf.getPath(), "DILITHIUM_SIGNATURE");

        if (rsaB64 == null || pqcB64 == null) {
            System.out.println("Δεν βρέθηκαν embedded signatures μέσα στο PDF (metadata).");
            System.out.println("Άρα είτε δεν υπέγραψες embedded, είτε υπέγραψες σε txt.");
            return;
        }

        byte[] rsaSigBytes = Base64.getDecoder().decode(rsaB64);
        byte[] pqSigBytes  = Base64.getDecoder().decode(pqcB64);


        // --- Verify RSA ---
        Signature rsaSig = Signature.getInstance("SHA256withRSA");
        rsaSig.initVerify(rsaPublic);
        rsaSig.update(toBeVerified);
        boolean rsaOK = rsaSig.verify(rsaSigBytes);

        // --- Verify PQC ---
        Signature pqSig = Signature.getInstance("DILITHIUM3", "BC");
        pqSig.initVerify(pqPublic);
        pqSig.update(toBeVerified);
        boolean pqOK = pqSig.verify(pqSigBytes);

        System.out.println("RSA verify: " + rsaOK);
        System.out.println("PQC verify: " + pqOK);
        System.out.println("OVERALL: " + (rsaOK && pqOK ? "OK" : "FAIL"));
    }
}
