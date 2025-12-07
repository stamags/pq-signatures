package converters;

import model.Tbluser;

import javax.annotation.ManagedBean;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import java.util.logging.Logger;


/**
 * Created by George Tsotzolas
 */
@ManagedBean
@FacesConverter(value = "userConverter")
public class UserConverter implements Converter {
    private static final Logger log = Logger.getLogger(UserConverter.class.getName());

    @Override
    public Object getAsObject(FacesContext ctx, UIComponent component,
                              String value) {
        // This will return the actual object representation
        Tbluser c = new Tbluser();
        try {
            c  =(Tbluser) db.dbTransactions.getObjectsByProperty(Tbluser.class.getCanonicalName(),"username", (String) value).get(0);
        }catch (Exception ex){
            log.info("----ERROR---"+ ex);
        }

        if(c !=null) {
            return c;
        }else {
            return null;
        }
    }

    @Override
    public String getAsString(FacesContext fc, UIComponent uic, Object o) {
        //This will return view-friendly output for the dropdown menu
        if(o!=null && o instanceof Tbluser) {
            Tbluser user = (Tbluser) o;
            if (user.getUsername() != null) {
                return user.getUsername();
            } else {
                return null;
            }
        }else{
            return null;
        }
    }
}
