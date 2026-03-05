package utils;

import model.Tbluser;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Calendar;

/**
 * Διαχείριση keystore ανά χρήστη.
 * Δομή: data/users/{sanitizedUsername}/keystore.p12 (RSA) + pqc-private.key, pqc-public.key (Dilithium).
 */
public class UserKeystoreService {

    private static final String USER_KEYS_SUBDIR = "users";
    private static final String KEYSTORE_FILE = "keystore.p12";
    private static final String PQC_PRIVATE_FILE = "pqc-private.key";
    private static final String PQC_PUBLIC_FILE = "pqc-public.key";
    private static final String RSA_PUBLIC_FILE = "rsa-public.key";
    /** Alias για το RSA entry στο keystore (χρήση και από KeyLoader). */
    public static final String RSA_ALIAS = "rsa";

    static {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /**
     * Βασικός κατάλογος για κλειδιά (ίδια λογική με KeyLoader).
     */
    private static Path getKeysBaseDir() {
        String keysDir = System.getProperty("pq.signatures.keys.dir");
        if (keysDir != null && !keysDir.isEmpty()) {
            return Paths.get(keysDir);
        }
        Path relativePath = Paths.get("data");
        if (Files.exists(relativePath)) {
            return relativePath.toAbsolutePath();
        }
        return Paths.get(System.getProperty("user.home"), "pq-signatures-keys", "data");
    }

    /**
     * Καθαρισμός username για χρήση ως όνομα φακέλου (αλφαριθμητικά, _, -).
     */
    public static String sanitizeUsername(String username) {
        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        return username.replaceAll("[^a-zA-Z0-9_\\-.@]", "_");
    }

    /**
     * Κατάλογος κλειδιών για τον χρήστη (π.χ. data/users/john_doe/).
     */
    public static Path getUserKeysDir(String username) {
        return getKeysBaseDir().resolve(USER_KEYS_SUBDIR).resolve(sanitizeUsername(username));
    }

    /**
     * Διαδρομή keystore (PKCS12) για RSA.
     */
    public static Path getKeystorePath(String username) {
        return getUserKeysDir(username).resolve(KEYSTORE_FILE);
    }

    /**
     * Διαδρομή αρχείου ιδιωτικού κλειδιού PQC.
     */
    public static Path getPqcPrivateKeyPath(String username) {
        return getUserKeysDir(username).resolve(PQC_PRIVATE_FILE);
    }

    /**
     * Διαδρομή αρχείου δημόσιου κλειδιού PQC.
     */
    public static Path getPqcPublicKeyPath(String username) {
        return getUserKeysDir(username).resolve(PQC_PUBLIC_FILE);
    }

    /**
     * Διαδρομή αρχείου δημόσιου κλειδιού RSA (για επαλήθευση χωρίς κωδικό).
     */
    public static Path getRsaPublicKeyPath(String username) {
        return getUserKeysDir(username).resolve(RSA_PUBLIC_FILE);
    }

    /**
     * Ελέγχει αν ο χρήστης έχει ήδη keystore και PQC κλειδιά.
     */
    public static boolean hasUserKeystore(String username) {
        Path keystore = getKeystorePath(username);
        Path pqcPriv = getPqcPrivateKeyPath(username);
        return Files.exists(keystore) && Files.exists(pqcPriv);
    }

    /**
     * Διαγράφει όλα τα κλειδιά του χρήστη (για περίπτωση "ξεχάσα τον κωδικό").
     * Μετά ο χρήστης μπορεί να δημιουργήσει νέα κλειδιά. Οι παλιές υπογραφές του δεν θα επαληθεύονται.
     */
    public static void deleteUserKeystore(String username) throws Exception {
        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }
        Path userDir = getUserKeysDir(username);
        if (!Files.exists(userDir)) {
            return;
        }
        try (Stream<Path> list = Files.list(userDir)) {
            for (Path p : list.toList()) {
                Files.deleteIfExists(p);
            }
        }
        Files.deleteIfExists(userDir);
    }

    /**
     * Δημιουργεί keystore και PQC αρχεία για τον χρήστη.
     * RSA: keystore.p12 με ιδιωτικό κλειδί + self-signed πιστοποιητικό.
     * PQC: pqc-private.key, pqc-public.key.
     *
     * @param user           ο χρήστης (για όνομα στο πιστοποιητικό)
     * @param keystorePassword κωδικός για το PKCS12 (δεν αποθηκεύεται πουθενά αλλού)
     */
    public static void createKeystoreForUser(Tbluser user, char[] keystorePassword) throws Exception {
        if (user == null || user.getUsername() == null) {
            throw new IllegalArgumentException("User and username are required");
        }
        String username = user.getUsername();
        Path userDir = getUserKeysDir(username);
        Files.createDirectories(userDir);

        // 1. RSA key pair + self-signed cert
        KeyPairGenerator rsaKpg = KeyPairGenerator.getInstance("RSA");
        rsaKpg.initialize(2048);
        KeyPair rsaKp = rsaKpg.generateKeyPair();

        String cn = buildCertificateCN(user);
        X509Certificate cert = createSelfSignedCertificate(rsaKp, cn);

        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, null);
        ks.setKeyEntry(RSA_ALIAS, rsaKp.getPrivate(), keystorePassword, new X509Certificate[]{cert});

        Path keystorePath = getKeystorePath(username);
        try (var fos = Files.newOutputStream(keystorePath)) {
            ks.store(fos, keystorePassword);
        }

        // Δημόσιο κλειδί RSA σε αρχείο (για επαλήθευση χωρίς κωδικό keystore)
        Files.writeString(getRsaPublicKeyPath(username),
                Base64.getEncoder().encodeToString(rsaKp.getPublic().getEncoded()) + "\n",
                StandardCharsets.UTF_8);

        // 2. PQC (Dilithium) key pair -> αρχεία
        KeyPairGenerator pqcKpg = KeyPairGenerator.getInstance("DILITHIUM3", "BC");
        KeyPair pqcKp = pqcKpg.generateKeyPair();

        String pqcPrivB64 = Base64.getEncoder().encodeToString(pqcKp.getPrivate().getEncoded());
        String pqcPubB64 = Base64.getEncoder().encodeToString(pqcKp.getPublic().getEncoded());

        Files.writeString(getPqcPrivateKeyPath(username), pqcPrivB64 + "\n", StandardCharsets.UTF_8);
        Files.writeString(getPqcPublicKeyPath(username), pqcPubB64 + "\n", StandardCharsets.UTF_8);
    }

    private static String buildCertificateCN(Tbluser user) {
        String name = user.getName() != null ? user.getName().trim() : "";
        String surname = user.getSurname() != null ? user.getSurname().trim() : "";
        if (!name.isEmpty() || !surname.isEmpty()) {
            return (name + " " + surname).trim();
        }
        return user.getUsername() != null ? user.getUsername() : "User";
    }

    private static X509Certificate createSelfSignedCertificate(KeyPair keyPair, String cn) throws Exception {
        X500Name subject = new X500Name("CN=" + cn + ", O=PQ-Signatures, C=GR");
        long now = System.currentTimeMillis();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, 10);

        X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(
                subject,
                BigInteger.valueOf(now),
                new java.util.Date(now),
                cal.getTime(),
                subject,
                SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded())
        );

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                .setProvider("BC")
                .build(keyPair.getPrivate());

        X509CertificateHolder certHolder = certBuilder.build(signer);
        return new JcaX509CertificateConverter().setProvider("BC").getCertificate(certHolder);
    }
}
