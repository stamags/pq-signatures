/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package beans;

import model.Tblelements;
import model.Tblpages;
import org.omnifaces.util.Ajax;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.List;

/**
 * @author Tsotzolas George
 */

@Named("PagesBean")
@ViewScoped
public class Pages implements Serializable {

    private List<Tblpages> pagesList;
    private String infomsg;
    private Tblpages page = new Tblpages();
    private Tblpages pageCopy = new Tblpages();

    //Login Bean
    @Inject
    private LoginBean loginBean;


    @PostConstruct
    public void init() {
        pagesList = (List<Tblpages>) (List<?>) db.dbTransactions.getAllObjectsSorted(Tblpages.class.getCanonicalName(), "pageName", 0);
        clear1();
    }

    /**
     * Αντιγράφει το Object για να μπορέσουμε να κάνουμε τυο Update.
     */
    public void copyObject() {
        pageCopy = page;
    }

    public void insert() {
        FacesMessage message = new FacesMessage();

        if (page != null) {
            db.dbTransactions.storeObject(page);

            //Φτιάνουμε και ένα default element για κάθε σελίδα
            Tblelements element = new Tblelements();
            element.setElementName("default");
            element.setElementDesc("default");
            element.setRefPageId(page);

            db.dbTransactions.storeWithMergeObject(element);


        } else {
            message = new FacesMessage(FacesMessage.SEVERITY_INFO, "Fault Insert", page.getPageName());
        }
        if (FacesContext.getCurrentInstance().getMaximumSeverity() == null) {

            pagesList.add(page);

            FacesMessage msg = new FacesMessage("Successful Insert", page.getPageName());
            FacesContext.getCurrentInstance().addMessage(null, msg);


        }
    }


    public void update() {
        FacesMessage message = new FacesMessage();

        if (page != null) {
            db.dbTransactions.updateObject(page);
        } else {
            message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fault Update", page.getPageName());
        }
        if (FacesContext.getCurrentInstance().getMaximumSeverity() == null) {

            int index = pagesList.indexOf(pageCopy);

            pagesList.set(index, page);

            FacesMessage msg = new FacesMessage("Successful Update", (page.getPageName()));
            FacesContext.getCurrentInstance().addMessage(null, msg);


        }
    }


    public void delete() {

        db.dbTransactions.deleteObject(page);

        if (FacesContext.getCurrentInstance().getMaximumSeverity() == null) {
            FacesMessage msg = new FacesMessage("Successful Delete");
            FacesContext.getCurrentInstance().addMessage(null, msg);

            pagesList.remove(page);
            Ajax.update("form");


        }
    }


    public void addMessage(String summary) {
        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, summary, null);
        FacesContext.getCurrentInstance().addMessage(null, message);
    }


    public void clear1() {
        page = new Tblpages();
    }




    public LoginBean getLoginBean() {
        return loginBean;
    }

    public void setLoginBean(LoginBean loginBean) {
        this.loginBean = loginBean;
    }

    public List<Tblpages> getPagesList() {
        return pagesList;
    }

    public void setPagesList(List<Tblpages> pagesList) {
        this.pagesList = pagesList;
    }

    public String getInfomsg() {
        return infomsg;
    }

    public void setInfomsg(String infomsg) {
        this.infomsg = infomsg;
    }

    public Tblpages getPage() {
        return page;
    }

    public void setPage(Tblpages page) {
        this.page = page;
    }

    public Tblpages getPageCopy() {
        return pageCopy;
    }

    public void setPageCopy(Tblpages pageCopy) {
        this.pageCopy = pageCopy;
    }
}

