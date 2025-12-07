package manhattan;

import beans.LoginBean;
import model.Tbluser;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;

@Named
@SessionScoped
public class GuestPreferences implements Serializable {

    private String layoutmode;

    private String theme;

    private boolean lightMenu;


    @Inject
    private LoginBean loginBean;


    public void saveTheme(String theme) {
        if (theme != null) {

            loginBean.getUser().setTheme(theme);
            Tbluser tbluser = new Tbluser();
            tbluser = loginBean.getUser();
            tbluser.setTheme(theme);
            db.dbTransactions.updateObject(tbluser);

        }
    }


    public void saveLightMenu(Boolean l) {
        if (l != null) {

            loginBean.getUser().setLightMenu(l);
            Tbluser tbluser = new Tbluser();
            tbluser = loginBean.getUser();
            tbluser.setLightMenu(l);
            db.dbTransactions.updateObject(tbluser);

        }
    }


    public void saveLayoutMode(String mode) {
        if (mode != null) {

            layoutmode = mode;
            loginBean.getUser().setLayoutMode(mode);
            Tbluser tbluser = new Tbluser();
            tbluser = loginBean.getUser();
            tbluser.setLayoutMode(mode);
            db.dbTransactions.updateObject(tbluser);

        }

    }


    public String getLayoutStyleClass() {

       layoutmode = loginBean.getUser().getLayoutMode();

        if(layoutmode == null){
            layoutmode = "slim";
        }

        String layoutStyleClass;
        switch (layoutmode) {
            case "slim":
                layoutStyleClass = "layout-slim";
                break;

            case "static":
                layoutStyleClass = "layout-static";
                break;

            case "overlay":
                layoutStyleClass = "layout-overlay";
                break;

            case "horizontal":
                layoutStyleClass = "layout-horizontal";
                break;

            case "toggle":
                layoutStyleClass = "layout-toggle";
                break;

            default:
                layoutStyleClass = "layout-slim";
                break;
        }

        //Εδώ ειναι για να ρυθμίζουμε για το dark και το light layoutmode
        if (loginBean.getUser().getLightMenu() != null && loginBean.getUser().getLightMenu()) {
            layoutStyleClass += " layout-light";
        } else {
            layoutStyleClass += " layout-dark";
        }

        return layoutStyleClass;
    }


    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public boolean isLightMenu() {
        return this.lightMenu;
    }

    public void setLightMenu(boolean value) {
        this.lightMenu = value;
    }


    public LoginBean getLoginBean() {
        return loginBean;
    }

    public void setLoginBean(LoginBean loginBean) {
        this.loginBean = loginBean;
    }

    public String getLayoutmode() {
        return layoutmode;
    }

    public void setLayoutmode(String layoutmode) {
        this.layoutmode = layoutmode;
    }

}