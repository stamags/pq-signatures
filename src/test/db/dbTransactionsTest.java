package db;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class dbTransactionsTest {

    private static Logger log = Logger.getLogger(dbTransactionsTest.class.getCanonicalName());

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void getObjectById() {

        log.info("test!!!!!!!!!!!!!!!!!!!!!!!!!1");
        Tblwagon tblwagon ;
        tblwagon = ((Tblwagon) dbTransactions.getObjectById(Tblwagon.class.getCanonicalName(), 1));

        System.out.println(tblwagon.getIdWagon());
        // assert statements
        assertEquals(1,tblwagon.getIdWagon().intValue());
        assertTrue(tblwagon.getIdWagon() == 1);
        assertTrue(tblwagon.getIdWagon() != null);
    }

    @Test
    public void getObjectsByProperties() {

        List <Tblwagon> wagonList = new ArrayList<>();

        Map<String, Object> wagonProperties = new HashMap<String, Object>();
        wagonProperties .put("wagonMode", 0);
        wagonProperties .put("wagonAxis", 6);

        wagonList = (List<Tblwagon>) (List<?>) dbTransactions.getObjectsByProperties(Tblwagon.class.getCanonicalName(), wagonProperties );

        assertTrue(wagonList .size() >= 1);

    }


    /**
     * This test is for getObjectsByProperties with page and pagesize
     */
    @Test
    public void getObjectsByProperties1() {

        List <Tblwagon> wagonList = new ArrayList<>();

        Map<String, Object> efedrosProperties = new HashMap<String, Object>();
        efedrosProperties.put("wagonMode", 0);

        wagonList = (List<Tblwagon>) (List<?>) dbTransactions.getObjectsByProperties(Tblwagon.class.getCanonicalName(), efedrosProperties,1,20);

        log.info(wagonList.size());
        assertTrue(wagonList.size() == 20);
    }

    /**
     * This test is for getObjectsByProperties with page ,pagesize and sortProperties
     */
    @Test
    public void getObjectsByProperties2() {
        List <Tblwagon> wagonList = new ArrayList<>();
        Map<String, Integer> sortproperties = new HashMap<>();
        sortproperties.put("wagonMode",0);

        Map<String, Object> efedrosProperties = new HashMap<String, Object>();
        efedrosProperties.put("wagonAxis", 6);

        wagonList = (List<Tblwagon>) (List<?>) dbTransactions.getObjectsByProperties(Tblwagon.class.getCanonicalName(), efedrosProperties,1,20,sortproperties);

        for (Tblwagon ef : wagonList ) {
            log.info(ef.getWagonPlate());
        }
        assertTrue(wagonList.size() == 20);
    }

    @Test
    public void getObjectsByProperties3() {
    }


//    @Test
//    public void executeProcedure() {
//        List <Tblwagon> efedrosList = new ArrayList<>();
//
//        List <Object> efedrosProperties = new ArrayList<>();
//        efedrosProperties.add("153153153");
//
//        efedrosList = (List<Tblwagon>) (List<?>) dbTransactions.executeProcedure("efedros_get", efedrosProperties);
//
//        assertTrue(efedrosList.size() >= 1);
//
//        efedrosList = new ArrayList<>();
//        efedrosProperties.add("Τιτάνας");
//        efedrosList = (List<Tblwagon>) (List<?>) dbTransactions.executeProcedure("efedros_many_prop", efedrosProperties);
//
//        assertTrue(efedrosList.size() >= 1);
//    }



    @Test
    public void countAllObjectsByProperties() {

        Integer wagonCount ;

        Map<String, Object> wagonProperties = new HashMap<String, Object>();
        wagonProperties.put("wagonMode", 0);

        wagonCount =  db.dbTransactions.countAllObjectsByProperties(Tblwagon.class.getCanonicalName(),wagonProperties);

        log.info(wagonCount);
        assertTrue(wagonCount == 150);

        wagonProperties.clear();

        wagonProperties.put("wagonMode", 0);
        wagonProperties.put("wagonAxis", 6);

        wagonCount =  db.dbTransactions.countAllObjectsByProperties(Tblwagon.class.getCanonicalName(),wagonProperties);

        log.info(wagonCount);
        assertTrue(wagonCount== 80);

    }

    @Test
    public void countAllObjectsByProperties1() {
    }

    @Test
    public void getObjectsBySqlQuery() {

        List <Tblwagon> wagonList = new ArrayList<>();

        String query = "from tblwagon e ";

        wagonList  = (List<Tblwagon>) (List<?>) dbTransactions.getObjectsBySqlQuery(Tblwagon.class, query, null, null, null);

        log.info(wagonList.size());

        assertTrue(wagonList.size() >= 1);

        query = "from tblwagon e where e. wagonMode= :param1 and e.wagonAxis = :param2";

        List <Object> wagonProperties = new ArrayList<>();
        wagonProperties.add(0);
        wagonProperties.add(6);

        wagonList = new ArrayList<>();

        wagonList = (List<Tblwagon>) (List<?>) dbTransactions.getObjectsBySqlQuery(Tblwagon.class, query, wagonProperties, 1, 1);

        assertTrue(wagonList.size() == 1);
    }

    @Test
    public void getObjectsBySqlQueryDistinct() {

        List <Tblthemes> themesList = new ArrayList<>();

        String query = "from (\n" +
                "select * from tblthemes\n" +
                "union all\n" +
                "select * from tblthemes os1) e ";

        themesList  = (List<Tblthemes>) (List<?>) dbTransactions.getObjectsBySqlQueryDistinct(Tblthemes.class, query, null, null, null);

        log.info(themesList.size());

        assertTrue(themesList.size() == 5);

        themesList =  new ArrayList<>();

        themesList  = (List<Tblthemes>) (List<?>) dbTransactions.getObjectsBySqlQueryDistinct(Tblthemes.class, query, null, 1, 10);

        assertTrue(themesList.size() == 5);
    }

//    @Test
//    public void countObjectsBySqlQuery() {
//
//        Integer count = 0;
//
//        String query = "from tblwagon e ";
//
//        count  =  dbTransactions.countObjectsBySqlQuery(Tblwagon.class,query,null);
//
//        log.info(count);
//
//        assertTrue(count > 1);
//
//
//        query = "from tblwagon e where e. wagonMode= :param1 and e.wagonAxis = :param2";
//
//        List <Object> wagonProperties = new ArrayList<>();
//        wagonProperties.add(0);
//        wagonProperties.add(6);
//
//        count = 0;
//
//        count  =  dbTransactions.countObjectsBySqlQuery(Tblwagon.class,query,wagonProperties);
//
//        log.info(count);
//
//        assertTrue(count == 1);
//
//
//
//    }
//
//    @Test
//    public void getObjectsBySqlQueryNew() {
//    }
//
//    @Test
//    public void getObjectsBySqlQueryObject() {
//
//        List <Object[]> objectArrayList = new ArrayList<>();
//
//        String query = "from ( select distinct os.*,  ef.STRA_EPWNYMO , ef.STRA_ONOMA from os os, ken_stratevmenos ef\n" +
//                "where ef.OS_KWD = os.OS_KWD) e";
//
//        objectArrayList  = (List<Object[]>) (List<?>) dbTransactions.getObjectsBySqlQueryObject(query);
//
//        log.info(objectArrayList.size());
//
//        for (Object[] ob : objectArrayList) {
//            log.info(ob[0]);
//            log.info(ob[1]);
//            log.info(ob[2]);
//        }
//
//
//    }

    @Test
    public void getAllObjects() {
    }

    @Test
    public void getObjectsPaginated() {
    }

    @Test
    public void getAllObjectsSortedDistinct2() {
    }

    @Test
    public void getAllObjectsSortedBykeyProperty() {
    }

    @Test
    public void getAllObjectsSorted() {
    }

    @Test
    public void countAllObjects() {
    }

    @Test
    public void countObjectsByProperty() {
    }

    @Test
    public void getMaxByProperty() {
    }

    @Test
    public void getObjectsByProperty() {
    }

    @Test
    public void getObjectsByPropertyPaginated() {
    }

    @Test
    public void getObjectsByPropertyLike() {
    }

    @Test
    public void storeObjectsDeleteObjects() {
    }

    @Test
    public void storeObjectFromSsoService() {
    }

    @Test
    public void storeObject() {
    }

    @Test
    public void updateObject() {

        List<Tblemployee> employeeList = new ArrayList<>();


        Tblemployee employee = new Tblemployee();
        employee.setEmployeeEmail("tsotzolas@gmail.com");
        employee.setEmployeeFname("George");
        employee.setEmployeeLname("Tsotzolas");

        dbTransactions.storeObject(employee);

        Auditing.getInstance().store(Auditing.INSERT, Tblemployee.class.getCanonicalName(), employee.toString());


        Map<String, Object> employeeProperties = new HashMap<String, Object>();
        employeeProperties.put("employeeFname", "George");
        employeeProperties.put("employeeLname", "Tsotzolas");

        employeeList = (List<Tblemployee>) (List<?>) dbTransactions.getObjectsByProperties(Tblemployee.class.getCanonicalName(), employeeProperties,1,20);

        assertTrue(employeeList.size() == 1);

        assertTrue(employeeList.get(0).getEmployeeFname().equals("George"));
        assertTrue(employeeList.get(0).getEmployeeLname().equals("Tsotzolas"));

//        employee = employeeList.get(0);
//        employee.setEmployeeFname("Takis");
//
//        dbTransactions.updateObject(employee);
//
//        assertTrue(employee.getEmployeeFname().equals("Takis"));

        db.dbTransactions.deleteObject(employee);


        Map<String, Object> employeeProperties1 = new HashMap<String, Object>();
        employeeProperties1.put("employeeFname", "George");
        employeeProperties1.put("employeeLname", "Tsotzolas");

        employeeList = (List<Tblemployee>) (List<?>) dbTransactions.getObjectsByProperties(Tblemployee.class.getCanonicalName(), employeeProperties1,1,20);

        assertTrue(employeeList.size() == 0);
    }

    @Test
    public void convertListToSet() {
    }

    @Test
    public void getObjectsByManyToOneLong() {
    }

    @Test
    public void getObjectsByManyToOneString() {
    }

    @Test
    public void deleteObject() {
    }

    @Test
    public void deleteObjectsByManyToOneLong() {
    }

    @Test
    public void getObjectsByManyToOneLongSorted() {
    }

    @Test
    public void getObjectsByKeyProperty() {
    }

    @Test
    public void getObjectsByKeyPropertySorted() {
    }

    @Test
    public void getObjectsByKeyPropertySorted1() {
    }

    @Test
    public void getObjectsByTwoKeyPropertySorted() {
    }

    @Test
    public void getObjectsByKeyPropertyAndPropertySortedByKeyProperty() {
    }

    @Test
    public void getObjectsByPropertySorted() {
    }

    @Test
    public void getObjectsByManyToOneStringSorted() {
    }

    @Test
    public void deleteObjectsBySqlQuery() {
    }
}