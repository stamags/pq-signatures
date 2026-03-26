# PQ Signatures

Σύστημα διαχείρισης και επαλήθευσης ψηφιακών υπογραφών σε PDF, με υποστήριξη **κλασικών (RSA)** και **post-quantum (PQC)** υπογραφών (Dilithium).

---

## Λειτουργίες

- **Upload & Υπογραφή:** Ανέβασμα PDF και υπογραφή με RSA, Dilithium (PQC) ή υβριδική (RSA + PQC). Οπτική εμφάνιση της υπογραφής στο έγγραφο.
- **Επαλήθευση:** Έλεγχος υπογραφών με βάση το ByteRange του PDF (RSA, PQC, incremental updates, coverage).
- **Email:** Αποστολή υπογεγραμμένου PDF μέσω email με προαιρετικό μήνυμα.
- **Ιστορικό Ενεργειών (Audit):** Καταγραφή όλων των ενεργειών (upload, sign, verify, email, download) ανά έγγραφο και χρήστη.
- **Dashboard:** Στατιστικά (έγγραφα, επαληθεύσεις, events ανά τύπο, κατανομή υπογραφών, events ανά ημέρα/χρήστη).
- **Διαχείριση κλειδιών / κωδικών:** Δημιουργία και διαχείριση προσωπικών RSA (keystore `.p12`) και PQC (Dilithium) κλειδιών ανά χρήστη μέσω της αντίστοιχης σελίδας.
- **Δικαιώματα:** Login και πρόσβαση ανά σελίδα (upload, verify, audit, dashboard, διαχείριση κλειδιών, διαχείριση χρηστών, προτιμήσεις χρήστη).

---

## Τεχνολογίες

| Τεχνολογία | Χρήση |
|------------|--------|
| Java 21 | Runtime |
| Apache Maven 3.9.12 | Build |
| JSF (Jakarta Faces) + PrimeFaces | Web UI |
| JPA / Hibernate | ORM |
| MySQL 8 | Βάση δεδομένων |
| WildFly 39.0.0.Final | Application Server |
| Apache PDFBox | PDF, ByteRange, υπογραφές |
| Bouncy Castle | RSA, Dilithium (PQC) |
| Simple Java Mail | Αποστολή email |

---

## Απαιτήσεις

| Απαίτηση | Έκδοση / σημείωση | Λήψη |
|----------|-------------------|------|
| **Java** | 21 (LTS) | [Eclipse Temurin 21](https://adoptium.net/temurin/releases/?version=21) · [Oracle JDK 21](https://www.oracle.com/java/technologies/downloads/#java21) |
| **Apache Maven** | 3.9.12 | [Λήψη (apache.org)](https://maven.apache.org/download.cgi) · [Αρχείο 3.9.12 (binaries)](https://archive.apache.org/dist/maven/maven-3/3.9.12/binaries/) |
| **WildFly** | 39.0.0.Final | [wildfly.org — Downloads](https://www.wildfly.org/downloads/) · [GitHub release 39.0.0.Final](https://github.com/wildfly/wildfly/releases/tag/39.0.0.Final) |
| **MySQL Server** | 8.x | [MySQL Community Server](https://dev.mysql.com/downloads/mysql/) |
| **MySQL Workbench** | (τελευταία σταθερή για MySQL 8) | [MySQL Workbench](https://dev.mysql.com/downloads/workbench/) |

---

## Δομή Project

```
pq-signatures/
├── src/main/java/
│   ├── beans/          # JSF managed beans (LoginBean, UploadsignBean, VerifyBean, AuditBean, DashboardBean, UserKeysBean, κλπ.)
│   ├── model/          # JPA entities (DocumentFile, DocumentSignature, TbAuditEvent, Tbluser, κλπ.)
│   ├── rest/           # REST API (DocumentService)
│   ├── utils/          # PdfSignatureVerifier, PdfSignatureEmbedder, EmailService, DocumentAuditService, FileStorageService, κλπ.
│   └── ...
├── src/main/resources/
│   └── META-INF/       # persistence.xml (JNDI datasource: java:jboss/datasources/pqsignatures)
├── src/main/webapp/
│   ├── webContent/     # XHTML σελίδες (uploadsign, verify, audit, dashboard, user-keys, κλπ.)
│   └── WEB-INF/        # web.xml, template, sidebar, topbar
├── data/               # Βάση καταλόγου κλειδιών· ανά χρήστη: users/{username}/ (keystore.p12, PQC keys, rsa-public.key)
└── pom.xml
```

---

## Εγκατάσταση & Εκτέλεση

### 1. Clone & Build

```bash
git clone https://github.com/stamags/pq-signatures.git
cd pq-signatures
mvn clean package
```

Το WAR θα δημιουργηθεί στο `target/pq-signatures.war`.

### 2. Βάση δεδομένων (MySQL)

1. Δημιουργήστε μια κενή βάση στο MySQL (π.χ. `pqsignatures`) με το charset που συμφωνείτε (συνήθως `utf8mb4`).
2. Εισάγετε το **παρεχόμενο αρχείο SQL** (dump/schema) που συνοδεύει την παράδοση της εφαρμογής, π.χ.:

```bash
mysql -u root -p pqsignatures < pqsignatures-schema.sql
```

Η εφαρμογή αναμένει JNDI όνομα **`java:jboss/datasources/pqsignatures`** (ορίζεται στο `persistence.xml`). Το όνομα της βάσης στο `connection-url` πρέπει να ταιριάζει με αυτή που δημιουργήσατε.

Βασικοί πίνακες (ενδεικτικά): `document_file`, `document_signature`, `tb_audit_event`, `tbluser`, `tblroles`, `tblpages`, κλπ.

### 3. Datasource στο WildFly

Η εφαρμογή συνδέεται μέσω JPA στο JNDI **`java:jboss/datasources/pqsignatures`**. Πρέπει να ορίσετε **και** JDBC driver για MySQL **και** το ίδιο το datasource.

#### 3.1. JDBC driver (MySQL Connector/J)

Ο WildFly δεν συμπεριλαμβάνει τον driver της MySQL. Τυπικά:

1. Κατεβάστε το [MySQL Connector/J](https://dev.mysql.com/downloads/connector/j/) (αρχείο `.jar`).
2. Δημιουργήστε JBoss module, π.χ. `com.mysql`, και τοποθετήστε εκεί το `.jar` (ή ακολουθήστε την τεκμηρίωση της έκδοσης WildFly για “JDBC driver as module”).
3. Στο **Admin Console** του WildFly: **Configuration → Subsystems → Datasources & Drivers → JDBC Drivers → Add**: επιλέξτε το module που μόλις φτιάξατε και ορίστε `Driver class names` = `com.mysql.cj.jdbc.Driver` (για Connector/J 8.x).

Αν προτιμάτε **CLI** (ενδεικτικά — προσαρμόστε διαδρομή/έκδοση jar):

```text
module add --name=com.mysql --resources=/path/to/mysql-connector-j-8.x.x.jar --dependencies=jakarta.api,jakarta.transaction.api
/subsystem=datasources/jdbc-driver=mysql:add(driver-name=mysql,driver-module-name=com.mysql,driver-class-name=com.mysql.cj.jdbc.Driver)
```

Μετά το reload/restart του server, ο driver εμφανίζεται ως `mysql` (ή όπως τον ονομάσατε).

#### 3.2. Δημιουργία datasource

**Μέσω Admin Console**

1. **Configuration → Subsystems → Datasources & Drivers → Datasources → Add**.
2. Επιλέξτε **MySQL** (ή “From template”) και δώστε όνομα π.χ. **`pqsignatures`**.
3. **JNDI name:** ακριβώς `java:jboss/datasources/pqsignatures` (πρέπει να ταυτίζεται με το `persistence.xml`).
4. **Connection URL:** π.χ. `jdbc:mysql://localhost:3306/pqsignatures?useUnicode=true&characterEncoding=UTF-8` (αν χρειάζεται `serverTimezone`, `useSSL=false` για dev, προσθέστε τα εδώ).
5. **Username / Password:** διαπιστευτήρια χρήστη MySQL με δικαιώματα στη βάση.
6. **Test connection** πριν το αποθηκεύσετε.

**Μέσω CLI** (ενδεικτικό — αντικαταστήστε host, βάση, χρήστη, κωδικό):

```bash
cd $WILDFLY_HOME/bin
./jboss-cli.sh --connect
```

```text
data-source add \
  --name=pqsignatures \
  --jndi-name=java:jboss/datasources/pqsignatures \
  --driver-name=mysql \
  --connection-url=jdbc:mysql://localhost:3306/pqsignatures?useUnicode=true\&characterEncoding=UTF-8 \
  --user-name=your_user \
  --password=your_password \
  --enabled=true
```

**Μέσω `standalone.xml`** (υποσύστημα `datasources`): ισοδύναμο XML με το παρακάτω — προσαρμόστε `user-name`, `password`, URL:

```xml
<datasource jndi-name="java:jboss/datasources/pqsignatures" pool-name="pqsignatures" enabled="true" use-java-context="true">
    <connection-url>jdbc:mysql://localhost:3306/pqsignatures?useUnicode=true&amp;characterEncoding=UTF-8</connection-url>
    <driver>mysql</driver>
    <security>
        <user-name>your_user</user-name>
        <password>your_password</password>
    </security>
</datasource>
```

Σε production συνιστάται έλεγχος σύνδεσης (`valid-connection-checker`, `exception-sorter` για MySQL) και ρύθμιση pool (`min-pool-size`, `max-pool-size`) σύμφωνα με τη φόρτωση.

### 4. Κλειδιά υπογραφής

Τα κλειδιά είναι **ανά χρήστη**, όχι ένα κοινό ζεύγος στο `data/`. Κάθε χρήστης δημιουργεί/διαχειρίζεται τα δικά του μέσω της σελίδας **Διαχείριση κλειδιών** (`user-keys`).

Αποθηκεύονται κάτω από:

`data/users/{όνομα-χρήστη-sanitized}/`

Συνήθως περιλαμβάνουν:

- `keystore.p12` — RSA ιδιωτικό/δημόσιο (alias `rsa`)
- `pqc-private.key`, `pqc-public.key` — Dilithium (PQC)
- `rsa-public.key` — δημόσιο RSA σε μορφή που χρησιμοποιεί η εφαρμογή

Ο βασικός κατάλογος μπορεί να αλλάξει με ιδιότητα συστήματος:

```
-Dpq.signatures.keys.dir=/path/to/keys
```

Αν δεν υπάρχει τοπικός φάκελος `data`, η εφαρμογή μπορεί να πέσει σε default κάτω από το home του χρήστη (`pq-signatures-keys/data`) — δείτε `UserKeystoreService` / `KeyLoader`.

### 5. Αποθήκευση αρχείων

Τα PDF αποθηκεύονται στο `~/pq-signatures-uploads/uploads/` (default). Για άλλο path:

```
-Dpq.signatures.storage.base=/path/to/storage
```

### 6. Email (προαιρετικό)

Για αποστολή email χρειάζεται ρύθμιση SMTP. Το `EmailService` χρησιμοποιεί Gmail SMTP. Ρυθμίστε τα credentials μέσω μεταβλητών περιβάλλοντος ή config (δεν συνιστάται hardcode σε production).

### 7. Deploy

Αντιγράψτε το `target/pq-signatures.war` στο `deploy/` του WildFly ή κάντε deploy μέσω admin console.

**Όριο upload (RequestTooBigException):** Το default όριο του WildFly για το μέγεθος του request είναι 2 MB. Για μεγαλύτερα PDF εκτελέστε από το `bin/` του WildFly:

```bash
./jboss-cli.sh --connect --file=/path/to/wildfly-increase-upload-limit.cli
```

Το script `wildfly-increase-upload-limit.cli` ορίζει όριο 50 MB. Μετά κάντε reload του server.

---

## Διαδρομές εφαρμογής

Βάση URL (τοπικά, default WildFly): `http://localhost:8080/pq-signatures` — οι διαδρομές παρακάτω προστίθενται μετά το context path.

| Σελίδα | URL |
|--------|-----|
| Login | `/login.jsf` |
| Upload & Υπογραφή | `/webContent/uploadsign.jsf` |
| Επαλήθευση / Email | `/webContent/verify.jsf` |
| Ιστορικό Ενεργειών | `/webContent/audit.jsf` |
| Dashboard / Στατιστικά | `/webContent/dashboard.jsf` |
| Διαχείριση κλειδιών / κωδικών | `/webContent/user-keys.jsf` |
| Όλοι οι χρήστες (διαχείριση λογαριασμών) | `/webContent/oloiOiXristes.jsf` |
| Προτιμήσεις χρήστη | `/webContent/userpreferences.jsf` |

### Δοκιμαστικός λογαριασμός με δημιουργία νέων χρηστών

Με το παρεχόμενο SQL/schema, ένας λογαριασμός που μπορεί να κάνει **login** και έχει πρόσβαση στη **δημιουργία νέων χρηστών** (σελίδα «Όλοι οι χρήστες») είναι:

| Πεδίο | Τιμή |
|--------|------|
| Όνομα χρήστη | `paraskevi` |
| Κωδικός | `Stamags1234!` |


---

## Άδεια

Διπλωματική εργασία – εκπαιδευτική χρήση.
