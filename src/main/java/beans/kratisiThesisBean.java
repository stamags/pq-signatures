package beans;

import com.google.zxing.*;

import db.dbTransactions;
import model.*;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.simplejavamail.api.email.Email;
import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.mailer.MailerBuilder;
//import org.simplejavamail.mailer.MailerBuilder;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by stamags1988@gmail.com on 09/12/2020
 */
@Named
@ViewScoped
public class kratisiThesisBean implements Serializable {


    private ParkingUser parkingUser = new ParkingUser();
    private List<ParkingUser> parkingUsersList = new ArrayList<>();
    private ParkingBooking parkingBooking = new ParkingBooking();
    private List<ParkingBooking> parkingBookingList = new ArrayList<>();
    private ParkingSlots parkingSlots = new ParkingSlots();
    private List<ParkingSlots> parkingSlotsList = new ArrayList<>();
    private ParkingSlots selectedParkingSlot = new ParkingSlots();
    private List<ParkingSlots> parkingSlotsListDistinct = new ArrayList<>();
    private Tbluser tbluser = new Tbluser();
    private ParkingUsersCar parkingUsersCar = new ParkingUsersCar();
    private List<ParkingUsersCar> parkingUsersCarList = new ArrayList<>();
    private Boolean kratisiEosEnable = false;
    private Boolean orofosEnable = false;
    private Boolean slotEnable = false;
    private List<ParkingFloor> parkingFloorList = new ArrayList<>();
    private String dateStartTimeToString;
    private String dateEndTimeToString;
    private Boolean storeNewCar = false;
    private String qrCodePath;
    private String mailText;
    private String qrCodePath1 = "asd";
    private Image image;
    private String fileName;
    private static final String QR_FOLDER_PATH = "/resources/assets_aerial/css/qr";
    private static final String WEBAPP_BASE_PATH = "C:/Users/PX/IdeaProjects/parking/src/main/webapp";
    private String qrCodeFileName = null;
    private Boolean isEpiskeptis = false;


    //Για τις ημερομηνίες
    private String date = new String();
    private Calendar mindate = Calendar.getInstance();


    @Inject
    private LoginBean loginBean;


    public kratisiThesisBean() {
    }


    @PostConstruct
    public void init() throws IOException {
        //Αν είναι επισκέπτης disable τα πεδία
        isEpiskeptis = false;


        tbluser = loginBean.getUser();
        if (tbluser.getIdRole().getIdRole().equals(4)) {
            isEpiskeptis = true;
        }
        //Για να παίρνουμε τον έλεγχο για τα calendar να μην μπορεί να βάλει ο άλλος ότι ημερομηνία θέλει
        //Για να παίρνουμε τον έλεγχο για τα calendar να μην μπορεί να βάλει ο άλλος ότι ημερομηνία θέλει
        setDate(new SimpleDateFormat("dd/MM/yyyy").format(Calendar.getInstance().getTime()));
        mindate.add(Calendar.YEAR, -10);
        mindate.getTime();

        parkingFloorList = ((List<ParkingFloor>) (List<?>) db.dbTransactions.getAllObjects(ParkingFloor.class.getCanonicalName()));

        String parkingUserCarQuery = "from parking_users_car e where e.idUser=" + tbluser.getIdUser() + ";";
        parkingUsersCarList = (List<ParkingUsersCar>) (List<?>) dbTransactions.getObjectsBySqlQuery(ParkingUsersCar.class, parkingUserCarQuery, null, null, null);

        parkingBooking.setSinolikoKostos(0);
        if (Objects.isNull(parkingUsersCarList) || parkingUsersCarList.size() == 0) {
            storeNewCar = true;
            parkingUsersCar = new ParkingUsersCar();
        } else {
            parkingUsersCar = parkingUsersCarList.get(0);
        }

    }

    public Date getMinDate1() {

        Date timeStart = parkingBooking.getStartTime();
        if (timeStart == null) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, 0);
            return cal.getTime(); // ή null
        } else {
            Calendar cal = Calendar.getInstance();
            cal.setTime(timeStart);
            cal.add(Calendar.MINUTE, 30); // Μπορείς να αλλάξεις σε HOUR, DAY κ.λπ.
            return cal.getTime();

        }
    }


    public Date getMinDate() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 0);
        return cal.getTime(); // ή null
    }


    public Date getMaxDate() {
        Calendar currenDate = Calendar.getInstance();
        currenDate.add(Calendar.MONTH, +1);
        return currenDate.getTime();

    }


    public void enableKratisiEos(AjaxBehaviorEvent event) {

        kratisiEosEnable = true;
    }


    public void onImerominiaSelect(AjaxBehaviorEvent event) {

        orofosEnable = true;

        dateStartTimeToString = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(parkingBooking.getStartTime());
        dateEndTimeToString = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(parkingBooking.getEndTime());
        String queryAvailableTheseis = "from parking_slots e " +
                " where  e.slot_id not in (select b.slot_id from parking_booking b where b.start_time<= '" + dateEndTimeToString + "' and b.end_time>= '" + dateStartTimeToString + "'  )  ";


        parkingSlotsList = (List<ParkingSlots>) (List<?>) dbTransactions.getObjectsBySqlQuery(ParkingSlots.class, queryAvailableTheseis, null, null, null);
        parkingSlotsListDistinct = parkingSlotsList.stream().distinct().collect(Collectors.toList());

        //Υπολογίζουμε και το κόστος
        ipologismosKostous();
    }


    public void onOrofosSelect(AjaxBehaviorEvent event) {
        slotEnable = true;

        String queryAvailableTheseis = "from parking_slots e " +
                " where e.floor_id = " + parkingSlots.getFloorId().getFloorId() + " and  e.slot_id not in (select b.slot_id from parking_booking b where b.start_time<= '" + dateEndTimeToString + "' and b.end_time>= '" + dateStartTimeToString + "'  )  ";


        parkingSlotsList = (List<ParkingSlots>) (List<?>) dbTransactions.getObjectsBySqlQuery(ParkingSlots.class, queryAvailableTheseis, null, null, null);
        parkingSlotsListDistinct = parkingSlotsList.stream().distinct().collect(Collectors.toList());


    }


    //  Υπολογίζουμε το συνολικό κόστος, είναι 3 ευρώ την ώρα
    public void ipologismosKostous() {
        DateTime startDate = new DateTime(parkingBooking.getStartTime());
        DateTime endDate = new DateTime(parkingBooking.getEndTime());
        Period period = new Period(startDate, endDate);

        //Επειδή πιανει την διαφορά ώρας μεταξύ μόνο μίας ημέρας , θα υπολογίσουμε και τισ μέρεσ μέσα!
        Integer sinolikoKostos = 0;
        if (period.getDays() >= 1) {
            if (period.getHours() > 0) {
                sinolikoKostos = (period.getDays() * 35) + (3 * period.getHours());
            } else {
                sinolikoKostos = period.getDays() * 35;
            }
        } else {
            sinolikoKostos = 3 * period.getHours();
        }

        //Ελάχιστη χρέωση
        if (sinolikoKostos < 3) {
            sinolikoKostos = 3;
        }
        parkingBooking.setSinolikoKostos(sinolikoKostos);


        FacesMessage facesMessage = new FacesMessage(FacesMessage.SEVERITY_INFO, "Το συνολικό κόστος της κράτησης σας θα είναι " + sinolikoKostos + " € (ευρώ)", "");
        FacesContext.getCurrentInstance().addMessage("fail", facesMessage);
    }

//    public String generateQRCode(String bookingDetails) {
//        try {
//            String qrCodeText = "Booking Details: " + bookingDetails;
//             fileName = UUID.randomUUID().toString() + ".png";
//
//            // Get real path inside webapp
//            String relativePath = "/resources/assets_aerial/css/qr/";
//            String realPath = "C:/Users/PX/IdeaProjects/parking/src/main/webapp" + relativePath + fileName;
//            String relativeWebPath = "/resources/assets_aerial/css/qr";
//            String absoluteFilePath = FacesContext.getCurrentInstance().getExternalContext().getRealPath(relativeWebPath) +"\\"+ fileName;

    /// /            String absoluteFilePath12 = System.getProperty("user.dir") + "\\src\\main\\webapp\\resources\\qr\\" + fileName;
    /// ///            String absoluteFilePath1 = "C:\\Users\\PX\\IdeaProjects\\parking\\src\\main\\webapp\\resources\\qr\\" + fileName;
    /// /            String absoluteFilePath1 = "C:\\Users\\PX\\IdeaProjects\\parking\\src\\main\\webapp\\resources\\qr\\" + fileName;
//
//            File qrFile = new File(realPath);
//            qrFile.getParentFile().mkdirs(); // Create directory if not exists
//
//            QRCodeWriter qrCodeWriter = new QRCodeWriter();
//            BitMatrix bitMatrix = qrCodeWriter.encode(qrCodeText, BarcodeFormat.QR_CODE, 200, 200);
//            MatrixToImageWriter.writeToPath(bitMatrix, "PNG", qrFile.toPath());
//            TimeUnit.SECONDS.sleep(10);
//            // Return relative path for UI
//            qrCodePath = realPath;
//
//
//            return qrCodePath;
//
//
//        } catch (WriterException | IOException e) {
//            e.printStackTrace();
//            return null;
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
//    }


    // Getter to return the dynamic QR Code path
    public String getQrCodePath() {
        return qrCodeFileName != null ? QR_FOLDER_PATH + "/" + qrCodeFileName : null;
    }


//    public String generateQRCodeBase64(String bookingDetails) {
//        try {
//            ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode(bookingDetails);
//            QRCodeWriter qrCodeWriter = new QRCodeWriter();
//            BitMatrix bitMatrix = qrCodeWriter.encode(byteBuffer.toString(), BarcodeFormat.QR_CODE, 200, 200);
//
//            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
//
//            return "data:image/png;base64," + Base64.getEncoder().encodeToString(outputStream.toByteArray());
//        } catch (Exception e) {
//            e.printStackTrace();
//            return null;
//        }
//    }


    public void store() {

        if (!tbluser.getIdRole().getIdRole().equals(4)) {


            parkingBooking.setUserId(tbluser);
            parkingBooking.setHasArrived(false);
            parkingBooking.setHasLeft(false);
            parkingBooking.setSlotId(parkingSlots);
            parkingBooking.setArithmosKikloforias(parkingUsersCar.getArithmosKikloforias());
            parkingBooking.setMarkaAutokinitou(parkingUsersCar.getMarkaAutokinitou());
            parkingUsersCar.setIdUser(tbluser);


            //Φτιάχνω τις ημερομηνίες για το email
            String dateStartToString = new SimpleDateFormat("dd-MM-yyyy").format(parkingBooking.getStartTime());
            String dateEndToString = new SimpleDateFormat("dd-MM-yyyy").format(parkingBooking.getEndTime());
            String dateStartTimeToString = new SimpleDateFormat("HH:mm:ss").format(parkingBooking.getStartTime());
            String dateEndTimeToString = new SimpleDateFormat("HH:mm:ss").format(parkingBooking.getEndTime());

            try {
                dbTransactions.storeObject(parkingBooking);
                if (storeNewCar) {
                    dbTransactions.storeObject(parkingUsersCar);
                } else {
                    dbTransactions.updateObject(parkingUsersCar);
                }

                //Το κείμενο από το mail
                mailText = "Η κράτησή σας με κωδικό " + parkingBooking.getBookingId() + " για την ημέρα " + dateStartToString + " από " + dateStartTimeToString + " έως την ημέρα " + dateEndToString +
                        " και ώρα " + dateEndTimeToString + " στον " + parkingBooking.getSlotId().getFloorId().getFloorId() + "ο Όροφο, στην θέση " + parkingBooking.getSlotId().getSlotNumber() + " είναι επιτυχής! Το συνολικό κόστος της κράτησης ανέρχεται στα " + parkingBooking.getSinolikoKostos() + " €. Σας περιμένουμε!";


                //generate qr code, το βάζω πιο πριν για να μπορώ να βάλω και το qr code μαζί
                generateQRCode(mailText);

                String realPath = WEBAPP_BASE_PATH + QR_FOLDER_PATH;
                File qrFile = new File(realPath, qrCodeFileName);
                byte[] qrBytes = Files.readAllBytes(qrFile.toPath());
                String contentId = "qrimage";
                //Για να δουλέψει πρέπει να δημιουργήσω app password μέσω του gmail. Και στο password στον mailbuilder θα βάζω αυτόν τον κωδικό.
                //Η παρακάτω υλοποίηση είναι με τον simple java mail https://www.baeldung.com/java-sjm-email
                Email email = EmailBuilder.startingBlank()
                        .from("Georgios Stamatis", "stamags1988@gmail.com")
                        .to("dimiPlas", tbluser.getEmail())
                        .withSubject("Booking Parking Slot")
                        .withPlainText(mailText)
                        .withHTMLText("<p>" + mailText + "</p><br/><p>QR Code:</p><img src='cid:contentId '>")
                        .withEmbeddedImage(contentId , qrBytes, "image/png")
                        .buildEmail();

//   .withEmbeddedImage(qrCodeFileName+".png", qrBytes, "qrCodeFileName.png")
                Mailer mailer = MailerBuilder
                        .withSMTPServer("smtp.gmail.com", 587, "stamags1988@gmail.com", "yobfxwipagewywxi")
                        .withDebugLogging(true)
                        .buildMailer();
                mailer.sendMail(email);


//            Ajax.update("form:qrCode");
//            PrimeFaces.current().executeScript("form:qrCode");
            } catch (Exception e) {
                System.out.println(e.toString());
            }

            if (FacesContext.getCurrentInstance().getMaximumSeverity() == null) {
                FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO, "Επιτυχής κράτηση Θέσης!", "Η ημέρα και ώρα κράτησης σας είναι :" + dateStartTimeToString);
                FacesContext.getCurrentInstance().addMessage(null, msg);
            }

            kratisiEosEnable = false;
            slotEnable = false;
            orofosEnable = false;
            parkingBooking = new ParkingBooking();

        }
        if (FacesContext.getCurrentInstance().getMaximumSeverity() == null) {
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_WARN, "Πρέπει να κάνετε login!", "");
            FacesContext.getCurrentInstance().addMessage(null, msg);
        }
    }

    public void generateQRCode(String text) {
        try {
            // Generate a unique file name for the QR code
            String uniqueFileName = "qrcode_" + UUID.randomUUID().toString() + ".png";
            String realPath = WEBAPP_BASE_PATH + QR_FOLDER_PATH;

            // Ensure the directory exists
            File directory = new File(realPath);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // Define full file path
            Path qrCodeFullPath = Paths.get(realPath, uniqueFileName);

            // Generate QR Code
            Hashtable<EncodeHintType, Object> hintMap = new Hashtable<>();
            hintMap.put(EncodeHintType.CHARACTER_SET, "UTF-8");

            BitMatrix matrix = new MultiFormatWriter().encode(
                    text, BarcodeFormat.QR_CODE, 300, 300, hintMap);

            MatrixToImageWriter.writeToPath(matrix, "PNG", qrCodeFullPath);
            TimeUnit.SECONDS.sleep(5);
            // Update the file name dynamically
            qrCodeFileName = uniqueFileName;
//            qrCodePath1 ="css/qr/"+ uniqueFileName;
////            qrCodePath1 = realPath + '/' + uniqueFileName;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    //GETTERS SETTERS


    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }


    public ParkingBooking getParkingBooking() {
        return parkingBooking;
    }

    public void setParkingBooking(ParkingBooking parkingBooking) {
        this.parkingBooking = parkingBooking;
    }


    public ParkingSlots getParkingSlots() {
        return parkingSlots;
    }

    public void setParkingSlots(ParkingSlots parkingSlots) {
        this.parkingSlots = parkingSlots;
    }

    public List<ParkingSlots> getParkingSlotsList() {
        return parkingSlotsList;
    }

    public void setParkingSlotsList(List<ParkingSlots> parkingSlotsList) {
        this.parkingSlotsList = parkingSlotsList;
    }


    public Tbluser getTbluser() {
        return tbluser;
    }

    public void setTbluser(Tbluser tbluser) {
        this.tbluser = tbluser;
    }

    public LoginBean getLoginBean() {
        return loginBean;
    }

    public void setLoginBean(LoginBean loginBean) {
        this.loginBean = loginBean;
    }

    public ParkingUsersCar getParkingUsersCar() {
        return parkingUsersCar;
    }

    public void setParkingUsersCar(ParkingUsersCar parkingUsersCar) {
        this.parkingUsersCar = parkingUsersCar;
    }

    public Boolean getKratisiEosEnable() {
        return kratisiEosEnable;
    }

    public void setKratisiEosEnable(Boolean kratisiEosEnable) {
        this.kratisiEosEnable = kratisiEosEnable;
    }

    public Boolean getOrofosEnable() {
        return orofosEnable;
    }

    public void setOrofosEnable(Boolean orofosEnable) {
        this.orofosEnable = orofosEnable;
    }

    public Boolean getSlotEnable() {
        return slotEnable;
    }

    public void setSlotEnable(Boolean slotEnable) {
        this.slotEnable = slotEnable;
    }

    public List<ParkingFloor> getParkingFloorList() {
        return parkingFloorList;
    }

    public void setParkingFloorList(List<ParkingFloor> parkingFloorList) {
        this.parkingFloorList = parkingFloorList;
    }

    public void setQrCodePath(String qrCodePath) {
        this.qrCodePath = qrCodePath;
    }

    public Image getImage() {
        return image;
    }

    public void setImage(Image image) {
        this.image = image;
    }

    public String getQrCodeFileName() {
        return qrCodeFileName;
    }

    public void setQrCodeFileName(String qrCodeFileName) {
        this.qrCodeFileName = qrCodeFileName;
    }

    public Boolean getEpiskeptis() {
        return isEpiskeptis;
    }

    public void setEpiskeptis(Boolean episkeptis) {
        isEpiskeptis = episkeptis;
    }
}
