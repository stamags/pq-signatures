package security;

import model.*;
import org.apache.log4j.Logger;

import java.io.Serializable;
import java.util.*;

/**
 * @author tsotzolas@gmail.com
 */
public class    Security implements Serializable {

    private static final Logger log = Logger.getLogger(Security.class.getName());

    private Tblroles role;
    private Map<String, Tblroles> rolesMap;


    private List<Tblpages> pagesList;
    private Map<String, Tblpages> pagesMap;

    private HashMap<String, List<Tblelements>> elements = new HashMap<String, List<Tblelements>>();
    private HashMap<String, Integer> pages = new HashMap<String, Integer>();

    private List<Tblelements> elementsList;
    private Map<String, Tblelements> elementsMap = new HashMap<>();

    private Map<String, Set<String>> jsfPageActions; //maps Page name -> Set Action name (Action = JsfElement με ref_page_id = - 1)

    private boolean admin = false;


    /**
     * Αρχικοποιεί ένα αντικείμενο Security
     *
     * @param user
     */
    public Security(Tbluser user) {
        //Άμα ο χρήστης ειναι ενεργός
        if (user.getUserActive() ==1) {

            role = new Tblroles();
            role = user.getIdRole();


            admin =  user.getIdRole().getRoleDesc().equals("Administrator");
            elements = new HashMap<String, List<Tblelements>>();
            pages = new HashMap<String, Integer>();


            fillPageElement(user);


            pagesList = new ArrayList<Tblpages>();

            elementsList = new ArrayList<Tblelements>();

        }
    }


    /**
     * Γεμίζει τις σελίδες και τα elemeνnts
     * @param user
     */
    private void fillPageElement(Tbluser user) {

        //Βρίσκουμε τις σελίδες όπου ο χρήστης δεν έχει δικαίομα να δεί.
        String queryPages = "from (select p.* from tblpages p , tblrolerights rr\n" +
                "where rr.idPage = p.idpage\n" +
                "and rr.idRole = " + user.getIdRole().getIdRole() + ") e";

        pagesList = (List<Tblpages>) (List<?>) db.dbTransactions.getObjectsBySqlQueryDistinct(Tblpages.class, queryPages, null, null, null);

        //Γεμίζουμε τα pages για να ξέρουμε αν ο χρήστης δεν έχει πρόσβαση σε αυτες τις σελίδες.
        for (Tblpages p : pagesList) {
            pages.put(p.getPageName(), p.getIdpage());
        }


        //Βρίσκουμε τα elements των σελίδων όπου ο χρήστης δεν έχει δικαίομα να δεί.
        String queryElements = "from tblelements e where e.element_name!='default'";

        elementsList = (List<Tblelements>) (List<?>) db.dbTransactions.getObjectsBySqlQueryDistinct(Tblelements.class, queryElements, null, null, null);

    }



    public static Logger getLog() {
        return log;
    }


    /**
     * Ελέγχει αν ο χρήστης έχει τη συγκεκριμένη σελίδα
     *
     * @param pageName
     */
    public boolean foundPageName(String pageName) {

        return isAdmin() || pages.containsKey(pageName);
    }

    /**
     * Ελέγχει άμα ο χρήστης έχει την κατάλληλη σελίδα . Χρησιμοποίται για τα menus
     * Άμα ο χρήστης έχει την συγκεκριμένη σελίδα για το menu θα βλέπει το menu o χρήστης
     * @param pagePath
     * @return
     */
    public Boolean foundPageAccess(String pagePath) {
        return isAdmin() || pages.containsKey(pagePath);
    }

    /**
     * Ελέγχει άμα ο χρήστης μπορεί να δει το menu.
     * Για να το κάνουμε αυτό κάνουμε το εξείς.
     * Ελέγχουμε αν έχει πρόσβαση σε τουλάχιστον μία σελίδα που έχει μέσα το mεnu.
     * Άμα σε κάποια έχει τότε αυξάνουμε τον counter
     * Άμα ο counter ειναι μεγαλύτερος απο το μηδεν τότε ο χρήστης θα πρέπει να δει το menu.
     * @param pagesString
     * @return
     */
    public Boolean hasMenuAccess(String pagesString) {
        Boolean hasAccess = false;
        List<String> pageList = Arrays.asList(pagesString.split(","));
        List<String> tempList = new ArrayList<>();
        for(String s : pageList){
            tempList.add(s.trim());
        }

        int counter = 0;
        for(String a : tempList){
            if(pages.containsKey(a)){
               counter++;
            }
        }
        if(counter >0){
            hasAccess = true;
        }

        return isAdmin() || hasAccess;
    }


    /**
     * Search for an element in a currentPage List
     * <p/>
     * Χρησημοποιείται απευθειας πανω στο xhtml ελεγχοντας αν το συγκεκριμενο element υπάρχει στο map των
     * elements που βλεπει ο χρηστης. Άμα υπάρχει τότε ο χρήστης δεν θα πρέπει να μπορέι να δέι το element
     *
     * @param elementName
     * @return false if element is found or true if not found
     */
    public boolean foundElementName(String elementName,String page) {
        boolean hasElement = false;

        String elementCheckquery = "from (select rr.*\n" +
                "from tblrolerights rr, tblpages p , tblelements e\n" +
                "where e.element_name!='default'\n" +
                "and rr.idRole = " + role.getIdRole() +"\n"+
                "and p.idpage = rr.idPage\n" +
                "and e.idelement = rr.idElement\n" +
                "and p.page_name = '"+page+"'\n" +
                "and e.element_name = '"+ elementName + "')e";

        List<Tblrolerights> rightsList = (List<Tblrolerights>) (List<?>) db.dbTransactions.getObjectsBySqlQueryDistinct(Tblrolerights.class, elementCheckquery, null, null, null);

        // Άμα και το element ειναι και για την σελίδα την οποία λεμε
        if(rightsList.size()>0){
            hasElement = true;
        }

        return isAdmin() || !(hasElement);
    }

    /**
     * Ελέγχει εάν ο χρήστης έχει μια ενέργεια σε μια συγκεκριμένη σελίδα
     *
     * @param viewId     το path από το οποίο βρίσκουμε το page name
     * @param actionName το όνομα της ενέργειας
     * @return false εάν είναι admin (δηλαδή το στοιχείο θα φαίνεται πάντα)
     *         true εάν έχει την ενέργεια στη τριπλέτα (= δεν πρέπει να τη βλέπει)
     */
    public boolean foundPageAction(String viewId, String actionName) {
        if (isAdmin()) {
            return false;
        }
        String pageName = viewId.substring(
                viewId.lastIndexOf('/') + 1,
                viewId.indexOf('.'));
        if (jsfPageActions.containsKey(pageName)) {
            return jsfPageActions.get(pageName).contains(actionName);
        }
        return true;
    }


    /**
     * @return the admin
     */
    public boolean isAdmin() {
        return admin;
    }



    public String getRolesString() {
        String s = "";
//        for (Tblroles roles : getTblrolesList()) {
//            s += roles.getRoleDesc() + " ";
//        }
        return s;
    }


    public Tblroles getRole() {
        return role;
    }

    public void setRole(Tblroles role) {
        this.role = role;
    }

    public Map<String, Tblroles> getRolesMap() {
        return rolesMap;
    }

    public void setRolesMap(Map<String, Tblroles> rolesMap) {
        this.rolesMap = rolesMap;
    }

    public List<Tblpages> getPagesList() {
        return pagesList;
    }

    public void setPagesList(List<Tblpages> pagesList) {
        this.pagesList = pagesList;
    }

    public Map<String, Tblpages> getPagesMap() {
        return pagesMap;
    }

    public void setPagesMap(Map<String, Tblpages> pagesMap) {
        this.pagesMap = pagesMap;
    }


    public HashMap<String, Integer> getPages() {
        return pages;
    }

    public void setPages(HashMap<String, Integer> pages) {
        this.pages = pages;
    }

    public List<Tblelements> getElementsList() {
        return elementsList;
    }

    public void setElementsList(List<Tblelements> elementsList) {
        this.elementsList = elementsList;
    }

    public Map<String, Tblelements> getElementsMap() {
        return elementsMap;
    }

    public void setElementsMap(Map<String, Tblelements> elementsMap) {
        this.elementsMap = elementsMap;
    }
    public void setAdmin(boolean admin) {
        this.admin = admin;
    }


}