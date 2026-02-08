package beans;


import db.HashingUtil;
import db.dbTransactions;
import jakarta.annotation.ManagedBean;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import model.*;
import org.apache.log4j.Logger;
import org.primefaces.PrimeFaces;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import java.io.*;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Named("missionstimelineBean")
@ViewScoped
public class MissionstimelineBean implements Serializable {

    private ParkingSlots parkingSlots;
    private List<ParkingSlots> parkingSLotsList;
    private BigInteger eleytheresTheseisParking;
    private BigInteger eleytheresTheseisParking1ouOrofou;
    private BigInteger eleytheresTheseisParking2ouOrofou;
    private BigInteger eleytheresTheseisParking3ouOrofou;
    private Long sinoloAnevasmenwnEggrafwn;
    private BigInteger sinoloYpogegrammenwnEggrafwnMeRSA;
    private BigInteger sinoloYpogegrammenwnEggrafwnMePQC;
    private BigInteger sinoloYpogegrammenwnEggrafwnHybridika;
    private String currenDate;
    private String currenDateFront;
    private List<ParkingFloor> parkingFloorList = new ArrayList<>();
    private Tbluser tbluser = new Tbluser();
    private ParkingUsersCar parkingUsersCar = new ParkingUsersCar();
    private String passWord1 = "";
    private String passWord2 = "";
    private Boolean isEpiskeptis = false;

    private String welcomeMsg;

    @Inject
    private LoginBean loginBean;


    private static final Logger log = Logger.getLogger(MissionstimelineBean.class);

    public MissionstimelineBean() {

    }

    @PostConstruct
    public void init() throws IOException {

        Calendar c = Calendar.getInstance();
        int timeOfDay = c.get(Calendar.HOUR_OF_DAY);

        tbluser = loginBean.getUser();

        if (tbluser.getIdRole().getIdRole().equals(4)) {
            isEpiskeptis = true;
        }


        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        currenDate = LocalDate.now().format(formatter);

        DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        currenDateFront = LocalDate.now().format(formatter1);

        if (timeOfDay >= 5 && timeOfDay < 12) {
            welcomeMsg = "Καλημέρα!";
        } else if (timeOfDay >= 12 && timeOfDay < 17) {
            welcomeMsg = "Καλό Μεσημέρι!";
        } else if (timeOfDay >= 17 && timeOfDay < 20) {
            welcomeMsg = "Καλό Απόγευμα!";
        } else if (timeOfDay >= 20 && timeOfDay < 24) {
            welcomeMsg = "Καλό Βράδυ!";
        } else if (timeOfDay >= 0 && timeOfDay < 5) {
            welcomeMsg = "Δεν είναι καλή ώρα για δουλειά!";
        }

        //Αν είναι καινούριος χρήστης του ανοίγουμε το dialog
        if (loginBean.getNewUserFromGoogle().equals(true)) {
            //Θα πρέπει να αποθηκεύσει καινούριο αυτοκίνητο οπότε δημιουργούμε καινούριο object
            parkingUsersCar = new ParkingUsersCar();
            parkingUsersCar.setIdUser(tbluser);

            PrimeFaces current = PrimeFaces.current();
            current.executeScript("PF('dlg3').show();");
        }

        //Αν είναι καινούριος χρήστης του ανοίγουμε το dialog
        if (loginBean.getNewUser().equals(true)) {
            //Θα πρέπει να αποθηκεύσει καινούριο αυτοκίνητο οπότε δημιουργούμε καινούριο object
            parkingUsersCar = new ParkingUsersCar();

            PrimeFaces current = PrimeFaces.current();
            current.executeScript("PF('dlg5').show();");
        }

        //Αν είναι καινούριος χρήστης του ανοίγουμε το dialog
        if (tbluser.getResetPassword().equals(true)) {
            PrimeFaces current = PrimeFaces.current();
            current.executeScript("PF('dlg4').show();");
        }

//        //Βρίσκουμε πόσες θέσεις είναι ελεύθερες. Όσα User_id δεν είναι μέσα στο πίνακα booking για την συγκεκριμένη ημερομηνία
//        String eleytheresTheseisParkingQuery = "select (e.p) from (select count(p.IS_AVAILABLE) as p\n" +
//                "from parking_slots p\n" +
//                "where   p.slot_id not in (select b.user_id from parking_booking b where b.start_time>='" + currenDate + "' ) ) e";
//
//        eleytheresTheseisParking = (BigInteger) db.dbTransactions.getObjectsBySqlQry1(eleytheresTheseisParkingQuery);
//
//        String eleytheresTheseisParking1ouOrofouQuery = "select (e.p) from (select count(p.IS_AVAILABLE) as p\n" +
//                "from parking_slots p\n" +
//                "where  p.FLOOR_ID = 1 AND   p.slot_id not in (select b.user_id from parking_booking b where b.start_time>='" + currenDate + "' ) ) e";
//
//
//        eleytheresTheseisParking1ouOrofou = (BigInteger) db.dbTransactions.getObjectsBySqlQry1(eleytheresTheseisParking1ouOrofouQuery);
//
//        String eleytheresTheseisParking2ouOrofouQuery = "select (e.p) from (select count(p.IS_AVAILABLE) as p\n" +
//                "from parking_slots p\n" +
//                "where  p.FLOOR_ID = 2 AND   p.slot_id not in (select b.user_id from parking_booking b where b.start_time>='" + currenDate + "' ) ) e";
//
//        eleytheresTheseisParking2ouOrofou = (BigInteger) db.dbTransactions.getObjectsBySqlQry1(eleytheresTheseisParking2ouOrofouQuery);
//
//        String eleytheresTheseisParking3ouOrofouQuery = "select (e.p) from (select count(p.IS_AVAILABLE) as p\n" +
//                "from parking_slots p\n" +
//                "where  p.FLOOR_ID = 3 AND   p.slot_id not in (select b.user_id from parking_booking b where b.start_time>='" + currenDate + "' ) ) e";
//
//        eleytheresTheseisParking3ouOrofou = (BigInteger) db.dbTransactions.getObjectsBySqlQry1(eleytheresTheseisParking3ouOrofouQuery);




        //Βρίσκουμε πόσες θέσεις είναι ελεύθερες. Όσα User_id δεν είναι μέσα στο πίνακα booking για την συγκεκριμένη ημερομηνία
        String sinoloAnevasmenwnEggrafwnQuery = "select (e.p)  from (select count(p.DOCUMENT_ID) AS P \n" +
                "from  pqsignatures.document_file p ) e" ;
        sinoloAnevasmenwnEggrafwn = (Long) dbTransactions.getObjectsBySqlQry1(sinoloAnevasmenwnEggrafwnQuery);

//        String eleytheresTheseisParking1ouOrofouQuery = "select (e.p) from (select count(p.IS_AVAILABLE) as p\n" +
//                "from parking_slots p\n" +
//                "where  p.FLOOR_ID = 1 AND   p.slot_id not in (select b.user_id from parking_booking b where b.start_time>='" + currenDate + "' ) ) e";
//
//
//        eleytheresTheseisParking1ouOrofou = (BigInteger) db.dbTransactions.getObjectsBySqlQry1(eleytheresTheseisParking1ouOrofouQuery);
//
//        String eleytheresTheseisParking2ouOrofouQuery = "select (e.p) from (select count(p.IS_AVAILABLE) as p\n" +
//                "from parking_slots p\n" +
//                "where  p.FLOOR_ID = 2 AND   p.slot_id not in (select b.user_id from parking_booking b where b.start_time>='" + currenDate + "' ) ) e";
//
//        eleytheresTheseisParking2ouOrofou = (BigInteger) db.dbTransactions.getObjectsBySqlQry1(eleytheresTheseisParking2ouOrofouQuery);
//
//        String eleytheresTheseisParking3ouOrofouQuery = "select (e.p) from (select count(p.IS_AVAILABLE) as p\n" +
//                "from parking_slots p\n" +
//                "where  p.FLOOR_ID = 3 AND   p.slot_id not in (select b.user_id from parking_booking b where b.start_time>='" + currenDate + "' ) ) e";
//
//        eleytheresTheseisParking3ouOrofou = (BigInteger) db.dbTransactions.getObjectsBySqlQry1(eleytheresTheseisParking3ouOrofouQuery);


    }

    public void storeObjectsFromGoogle() {

        if (!passWord1.equals(passWord2)) {
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Ο κωδικός δεν είναι ίδιος και στα 2 πεδία!", "");
            FacesContext.getCurrentInstance().addMessage(null, msg);
            return;
        } else {
            tbluser.setPassword(HashingUtil.SHA1(passWord1));

            try {
                dbTransactions.updateObject(tbluser);
                dbTransactions.storeObject(parkingUsersCar);

                FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO, "Επιτυχής αποθήκευση των δεδομένων σας!", "");
                FacesContext.getCurrentInstance().addMessage(null, msg);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void storeObjectsNewUser() {

        if (!passWord1.equals(passWord2)) {
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Ο κωδικός δεν είναι ίδιος και στα 2 πεδία!", "");
            FacesContext.getCurrentInstance().addMessage(null, msg);
            return;
        } else {
            tbluser.setPassword(HashingUtil.SHA1(passWord1));

            try {
                dbTransactions.storeObject(tbluser);
                //Μόλις αποθηκευτει θα δημιουρηθεί και το Iduser
                parkingUsersCar.setIdUser(tbluser);
                dbTransactions.storeObject(parkingUsersCar);

                loginBean.setUser(tbluser);

                FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO, "Επιτυχής αποθήκευση των δεδομένων σας!", "");
                FacesContext.getCurrentInstance().addMessage(null, msg);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void updatePassword() {

        if (!passWord1.equals(passWord2)) {
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Ο κωδικός δεν είναι ίδιος και στα 2 πεδία!", "");
            FacesContext.getCurrentInstance().addMessage(null, msg);
            return;
        } else {
            tbluser.setPassword(HashingUtil.SHA1(passWord1));
            tbluser.setResetPassword(false);
            try {
                dbTransactions.updateObject(tbluser);

                FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO, "Επιτυχής αλλλαγή του Κωδικού!", "");
                FacesContext.getCurrentInstance().addMessage(null, msg);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public BigInteger getEleytheresTheseisParking() {
        return eleytheresTheseisParking;
    }

    public void setEleytheresTheseisParking(BigInteger eleytheresTheseisParking) {
        this.eleytheresTheseisParking = eleytheresTheseisParking;
    }

    public BigInteger getEleytheresTheseisParking1ouOrofou() {
        return eleytheresTheseisParking1ouOrofou;
    }

    public void setEleytheresTheseisParking1ouOrofou(BigInteger eleytheresTheseisParking1ouOrofou) {
        this.eleytheresTheseisParking1ouOrofou = eleytheresTheseisParking1ouOrofou;
    }

    public BigInteger getEleytheresTheseisParking2ouOrofou() {
        return eleytheresTheseisParking2ouOrofou;
    }

    public void setEleytheresTheseisParking2ouOrofou(BigInteger eleytheresTheseisParking2ouOrofou) {
        this.eleytheresTheseisParking2ouOrofou = eleytheresTheseisParking2ouOrofou;
    }

    public BigInteger getEleytheresTheseisParking3ouOrofou() {
        return eleytheresTheseisParking3ouOrofou;
    }

    public void setEleytheresTheseisParking3ouOrofou(BigInteger eleytheresTheseisParking3ouOrofou) {
        this.eleytheresTheseisParking3ouOrofou = eleytheresTheseisParking3ouOrofou;
    }

    public String getCurrenDate() {
        return currenDate;
    }

    public void setCurrenDate(String currenDate) {
        this.currenDate = currenDate;
    }

    public String getCurrenDateFront() {
        return currenDateFront;
    }

    public void setCurrenDateFront(String currenDateFront) {
        this.currenDateFront = currenDateFront;
    }

    public LoginBean getLoginBean() {
        return loginBean;
    }

    public void setLoginBean(LoginBean loginBean) {
        this.loginBean = loginBean;
    }

    public Tbluser getTbluser() {
        return tbluser;
    }

    public void setTbluser(Tbluser tbluser) {
        this.tbluser = tbluser;
    }

    public ParkingUsersCar getParkingUsersCar() {
        return parkingUsersCar;
    }

    public void setParkingUsersCar(ParkingUsersCar parkingUsersCar) {
        this.parkingUsersCar = parkingUsersCar;
    }

    public String getPassWord2() {
        return passWord2;
    }

    public void setPassWord2(String passWord2) {
        this.passWord2 = passWord2;
    }

    public String getPassWord1() {
        return passWord1;
    }

    public void setPassWord1(String passWord1) {
        this.passWord1 = passWord1;
    }


    public String getWelcomeMsg() {
        return welcomeMsg;
    }

    public void setWelcomeMsg(String welcomeMsg) {
        this.welcomeMsg = welcomeMsg;
    }

    public Boolean getEpiskeptis() {
        return isEpiskeptis;
    }

    public void setEpiskeptis(Boolean episkeptis) {
        isEpiskeptis = episkeptis;
    }

    public ParkingSlots getParkingSlots() {return parkingSlots;}

    public void setParkingSlots(ParkingSlots parkingSlots) {this.parkingSlots = parkingSlots;}

    public List<ParkingSlots> getParkingSLotsList() {return parkingSLotsList;}

    public void setParkingSLotsList(List<ParkingSlots> parkingSLotsList) {this.parkingSLotsList = parkingSLotsList;}

    public List<ParkingFloor> getParkingFloorList() {return parkingFloorList;}

    public void setParkingFloorList(List<ParkingFloor> parkingFloorList) {this.parkingFloorList = parkingFloorList;}


    public Long getSinoloAnevasmenwnEggrafwn() {
        return sinoloAnevasmenwnEggrafwn;
    }

    public void setSinoloAnevasmenwnEggrafwn(Long sinoloAnevasmenwnEggrafwn) {
        this.sinoloAnevasmenwnEggrafwn = sinoloAnevasmenwnEggrafwn;
    }

    public BigInteger getSinoloYpogegrammenwnEggrafwnMeRSA() {
        return sinoloYpogegrammenwnEggrafwnMeRSA;
    }

    public void setSinoloYpogegrammenwnEggrafwnMeRSA(BigInteger sinoloYpogegrammenwnEggrafwnMeRSA) {
        this.sinoloYpogegrammenwnEggrafwnMeRSA = sinoloYpogegrammenwnEggrafwnMeRSA;
    }

    public BigInteger getSinoloYpogegrammenwnEggrafwnMePQC() {
        return sinoloYpogegrammenwnEggrafwnMePQC;
    }

    public void setSinoloYpogegrammenwnEggrafwnMePQC(BigInteger sinoloYpogegrammenwnEggrafwnMePQC) {
        this.sinoloYpogegrammenwnEggrafwnMePQC = sinoloYpogegrammenwnEggrafwnMePQC;
    }

    public BigInteger getSinoloYpogegrammenwnEggrafwnHybridika() {
        return sinoloYpogegrammenwnEggrafwnHybridika;
    }

    public void setSinoloYpogegrammenwnEggrafwnHybridika(BigInteger sinoloYpogegrammenwnEggrafwnHybridika) {
        this.sinoloYpogegrammenwnEggrafwnHybridika = sinoloYpogegrammenwnEggrafwnHybridika;
    }
}
