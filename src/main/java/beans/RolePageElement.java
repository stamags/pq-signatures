/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package beans;

import db.dbTransactions;
import model.*;
import org.omnifaces.util.Ajax;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Tsotzolas George
 */

@Named("RolePageElementBean")
@ViewScoped
public class RolePageElement implements Serializable {

    private List<Tblroles> rolesList;
    private Tblroles role = new Tblroles();
    private String infomsg;
    private List<Tblpages> targetPagesList;
    private List<Tblpages> sourcePagesList;

    private List<Tblelements> targetElementsList;
    private List<Tblelements> sourceElementsList;
    private Tblpages selectedPage;
    private Tblelements selectedElement;

    //Login Bean
    @Inject
    private LoginBean loginBean;


    @PostConstruct
    public void init() {

        String rolesQuery = "from tblroles e where e.RoleDesc != 'Administrator' order by e.RoleDesc asc";
        rolesList = (List<Tblroles>) (List<?>) db.dbTransactions.getObjectsBySqlQuery(Tblroles.class, rolesQuery, null, null, null);
        clear1();
    }


    public void onRoleSelect() {
        String q1 = "from (select p.*\n" +
                "from tblpages p\n" +
                "where p.idpage not in (select distinct p.idpage\n" +
                "                     from tblrolerights rr ,tblpages p\n" +
                "                     where p.idpage = rr.idPage\n" +
                "                     and rr.idRole = " + role.getIdRole() +
                "))e";

        sourcePagesList = (List<Tblpages>) (List<?>) db.dbTransactions.getObjectsBySqlQuery(Tblpages.class, q1, null, null, null);


        String q = "from (select distinct p.* \n" +
                "from tblrolerights rr ,tblpages p\n" +
                "where p.idpage = rr.idPage " +
                "and rr.idRole = " + role.getIdRole() +
                ") e";
        targetPagesList = (List<Tblpages>) (List<?>) db.dbTransactions.getObjectsBySqlQuery(Tblpages.class, q, null, null, null);
    }


    public void onSelection(Tblpages page) {
        selectedPage = page;

        sourceElementsList = page.getTblelementsList();

        //Αφαιρούμε απο την λίστα τα default Elements
        sourceElementsList = sourceElementsList.stream().filter(i -> !i.getElementName().equals("default")).collect(Collectors.toList());


        String q = "from (select el.*\n" +
                "from tblrolerights rr,\n" +
                "     tblpages p,\n" +
                "     tblelements el\n" +
                "where p.idpage = rr.idPage\n" +
                "  and el.idelement = rr.idElement\n" +
                "  and rr.idPage = " + selectedPage.getIdpage() + "\n" +
                "  and rr.idRole = " + role.getIdRole() + "\n" +
                "  and el.element_name <> 'default'" +
                ") e";
        targetElementsList = (List<Tblelements>) (List<?>) db.dbTransactions.getObjectsBySqlQuery(Tblelements.class, q, null, null, null);

        //Αφαιρούμε απο την λίστα τοy source αυτά τα οποία υπάρχουν στο target άμα έχει δη κάποια elements
        if (targetElementsList.size() > 0) {
            sourceElementsList.removeAll(targetElementsList);
        }
    }


    public void onDBSelection(Tblpages selectedPage, List<Tblpages> listToAdd, List<Tblpages> listToRemove) {

        listToAdd.add(selectedPage);

        listToRemove.remove(selectedPage);

        Ajax.update("sourceTable , targetTable");

    }


    public void onDBElementsSelection(Tblelements selElememt, List<Tblelements> listElementToAdd, List<Tblelements> listElementToRemove) {

        listElementToAdd.add(selElememt);

        listElementToRemove.remove(selElememt);

    }


    public void addElementsToModel() {
        //Έχουμε την τριπλέτα
        //  Role    Page   Element
        //    1       1       2
        // Ο ρόλος ειναι κάτι τον οποίο έχει επιλέξει ο χρήστης απο την αρχή. Οπότε ειναι κάτι το οποίο ειναι σταθερό.
        // Αυτό το οποίο αλλάζει ειναι οι σελίδες και τα elements


        //Παίρνουμε όλα τα δεδομενα τα οποία είδη έχει μέσα ο χρηστης στην βάση,
        List<Tblrolerights> oldRolesRightsList = new ArrayList<>();

        String q = "from Tblrolerights e where e.idRole =" + role.getIdRole();

        oldRolesRightsList = (List<Tblrolerights>) (List<?>) db.dbTransactions.getObjectsBySqlQuery(Tblrolerights.class, q, null, null, null);


        //Στη συνέχεια θα πρέπει να σβήσουμε αυτά τα παλαιά δεδομένα απο την βαση δεδομένων.
        if (oldRolesRightsList.size() > 0) {

            //Σβήνουμε όλα τα διακαιώματα εκτός απο τα default elements
            String queryDelete = "Tblrolerights  where idRole = " + role.getIdRole() + ")";

            dbTransactions.deleteObjectsBySqlQuery(null, queryDelete);
        }

        if (FacesContext.getCurrentInstance().getMaximumSeverity() == null) {

            for (Tblpages page : targetPagesList) {
                List<Tblelements> pageElementList = new ArrayList<>();

                //Βρίσκουμε τα elements τα οποία έχουν αυτές οι σελίδες.
                pageElementList = page.getTblelementsList();

                //Απο αυτά τα elements θα πρέπει να βρούμε αυτα τα οποιά υπάρχουν και μέσα στο targetElementsList και να βάλκουμε και τα default
                List<Tblelements> sameElements = new ArrayList<>();
                for (Tblelements e : pageElementList) {
                    if (targetElementsList != null && targetElementsList.contains(e)) {
                        sameElements.add(e);

                    }
                    //Bάζουμε και το default element της σελίδας.
                    sameElements.add(page.getTblelementsList().stream().filter(i -> i.getElementName().equals("default")).collect(Collectors.toList()).get(0));
                }

                // Και τώρα πάμε να κάνουμε το insert
                // Στην περίπτωση που η σελίδα έχει elements
                if (sameElements.size() > 0) {
                    Tblrolerights roleright = new Tblrolerights();
                    for (Tblelements element : sameElements) {

                        TblrolerightsPK pk = new TblrolerightsPK();
                        pk.setIdElement(element.getIdelement());
                        pk.setIdPage(element.getRefPageId().getIdpage());
                        pk.setIdRole(role.getIdRole());

                        roleright.setTblpages(element.getRefPageId());
                        roleright.setTblroles(role);
                        roleright.setTblelements(element);
                        roleright.setTblrolerightsPK(pk);


                        List<Tblrolerights> inTheDBList = new ArrayList<>();

                        String q1 = "from Tblrolerights e where e.idRole =" + role.getIdRole() + " and e.idPage = " + page.getIdpage();

                        inTheDBList = (List<Tblrolerights>) (List<?>) db.dbTransactions.getObjectsBySqlQuery(Tblrolerights.class, q1, null, null, null);


                        if (!inTheDBList.contains(roleright)) {

                            db.dbTransactions.storeObject(roleright);
                        }
                    }

                    if (FacesContext.getCurrentInstance().getMaximumSeverity() == null) {
                        FacesMessage msg = new FacesMessage("Successful Insert", "Pages in the role " + role.getRoleDesc());
                        FacesContext.getCurrentInstance().addMessage(null, msg);


                    }
                }

            }
        }
    }

//    public void moveAllRight() {
//        targetPagesList = new ArrayList<>();
//        onRoleSelect();
//        targetPagesList = sourcePagesList;
//        sourcePagesList = new ArrayList<>();
//
//        Ajax.update("source,target");
//    }
//
//    public void moveAlLeft() {
//
//        sourcePagesList = new ArrayList<>();
//        onRoleSelect();
//        targetPagesList = new ArrayList<>();
//
//        Ajax.update("source,target");
//    }


    public void clear1() {
        role = new Tblroles();
        targetElementsList = new ArrayList<>();
        sourceElementsList = new ArrayList<>();
        selectedElement = new Tblelements();
        selectedPage = new Tblpages();
        targetPagesList = new ArrayList<>();
        sourcePagesList = new ArrayList<>();
    }


    public LoginBean getLoginBean() {
        return loginBean;
    }

    public void setLoginBean(LoginBean loginBean) {
        this.loginBean = loginBean;
    }


    public List<Tblroles> getRolesList() {
        return rolesList;
    }

    public void setRolesList(List<Tblroles> rolesList) {
        this.rolesList = rolesList;
    }

    public Tblroles getRole() {
        return role;
    }

    public void setRole(Tblroles role) {
        this.role = role;
    }

    public List<Tblpages> getTargetPagesList() {
        return targetPagesList;
    }

    public void setTargetPagesList(List<Tblpages> targetPagesList) {
        this.targetPagesList = targetPagesList;
    }

    public List<Tblpages> getSourcePagesList() {
        return sourcePagesList;
    }

    public void setSourcePagesList(List<Tblpages> sourcePagesList) {
        this.sourcePagesList = sourcePagesList;
    }


    public Tblpages getSelectedPage() {
        return selectedPage;
    }

    public void setSelectedPage(Tblpages selectedPage) {
        this.selectedPage = selectedPage;
    }


    public List<Tblelements> getTargetElementsList() {
        return targetElementsList;
    }

    public void setTargetElementsList(List<Tblelements> targetElementsList) {
        this.targetElementsList = targetElementsList;
    }

    public List<Tblelements> getSourceElementsList() {
        return sourceElementsList;
    }

    public void setSourceElementsList(List<Tblelements> sourceElementsList) {
        this.sourceElementsList = sourceElementsList;
    }

    public Tblelements getSelectedElement() {
        return selectedElement;
    }

    public void setSelectedElement(Tblelements selectedElement) {
        this.selectedElement = selectedElement;
    }
}

