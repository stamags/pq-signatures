package utils;

import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.SignerInfoGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import signatures.KeyLoader;

import java.io.IOException;
import java.io.InputStream;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Υλοποίηση SignatureInterface για RSA υπογραφή χρησιμοποιώντας PDFBox.
 * Αυτό δημιουργεί μια σωστή PDF cryptographic signature (PAdES compatible) με CMS/PKCS#7.
 */
public class RsaSignatureInterface implements SignatureInterface {

    private final PrivateKey privateKey;
    private final X509Certificate certificate;

    static {
        // Εξασφάλιση ότι ο BouncyCastle provider είναι διαθέσιμος
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public RsaSignatureInterface() throws Exception {
        this.privateKey = KeyLoader.loadPrivateKey("data/rsa-private.key", "RSA");
        this.certificate = createSelfSignedCertificate(privateKey);
    }

    /**
     * Δημιουργεί ένα self-signed X509 certificate από το private key.
     * Αυτό είναι απαραίτητο για PAdES υπογραφές.
     */
    private X509Certificate createSelfSignedCertificate(PrivateKey privateKey) throws Exception {
        PublicKey publicKey = KeyLoader.loadPublicKey("data/rsa-public.key", "RSA");

        // Χρήση BouncyCastle για δημιουργία self-signed certificate
        X509v3CertificateBuilder certBuilder =
                new X509v3CertificateBuilder(
                        new X500Name("CN=PQ-Signatures System, O=PQ-Signatures, C=GR"),
                        getSerialNumber(),
                        Calendar.getInstance().getTime(),
                        getExpiryDate(),
                        new X500Name("CN=PQ-Signatures System, O=PQ-Signatures, C=GR"),
                        SubjectPublicKeyInfo.getInstance(
                                publicKey.getEncoded())
                );

        ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256withRSA")
                .setProvider("BC")
                .build(privateKey);

        X509CertificateHolder certHolder = certBuilder.build(contentSigner);
        return new JcaX509CertificateConverter()
                .setProvider("BC")
                .getCertificate(certHolder);
    }

    private java.math.BigInteger getSerialNumber() {
        return java.math.BigInteger.valueOf(System.currentTimeMillis());
    }

    private java.util.Date getExpiryDate() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, 10); // 10 χρόνια validity
        return cal.getTime();
    }

    @Override
    public byte[] sign(InputStream content) throws IOException {
        try {
            // Ανάγνωση περιεχομένου προς υπογραφή
            byte[] contentBytes = content.readAllBytes();

            // Δημιουργία CMS SignedData με BouncyCastle
            CMSSignedDataGenerator generator = new CMSSignedDataGenerator();

            // Προσθήκη signer με certificate
            ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                    .setProvider("BC")
                    .build(privateKey);

            SignerInfoGenerator signerInfoGenerator = new JcaSignerInfoGeneratorBuilder(
                    new JcaDigestCalculatorProviderBuilder().setProvider("BC").build())
                    .build(signer, certificate);

            generator.addSignerInfoGenerator(signerInfoGenerator);

            // Προσθήκη certificate στο CMS
            List<X509Certificate> certList = new ArrayList<>();
            certList.add(certificate);
            JcaCertStore certStore = new JcaCertStore(certList);
            generator.addCertificates(certStore);

            // Δημιουργία CMS SignedData
            CMSSignedData signedData = generator.generate(
                    new org.bouncycastle.cms.CMSProcessableByteArray(contentBytes),
                    false // detached signature
            );

            // Επιστροφή DER-encoded CMS SignedData
            return signedData.getEncoded();
        } catch (Exception e) {
            throw new IOException("Αποτυχία υπογραφής περιεχομένου", e);
        }
    }
}
