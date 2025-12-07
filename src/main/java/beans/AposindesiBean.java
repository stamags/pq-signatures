package beans;

import java.io.Serializable;
import javax.annotation.PostConstruct;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

/**
 *
 * @author stamags@gmail.com
 */
@Named("AposindesiBean")
@ViewScoped
public class AposindesiBean implements Serializable {

    private static final Logger log = Logger.getLogger(AposindesiBean.class.getName());


    /**
     * Creates a new instance of aposindesiBean
     */
    public AposindesiBean() {
    }

    @PostConstruct
    public void init() {
    }

    public String  aposindesiUser() {

        // Ακύρωση του Session
        FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        request.getSession().setAttribute(LoginBean.AUTH_KEY, null);

        return "/login.jsf?faces-redirect=true";
    }

    public String changePassword() {
        return "/login/changePassword.jsf?faces-redirect=true";
    }
}
