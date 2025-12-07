package converters;

import model.Tblroles;

import jakarta.annotation.ManagedBean;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;
import java.util.logging.Logger;


/**
 * Created by George Tsotzolas
 */
@ManagedBean
@FacesConverter(value = "roleConverter")
public class RoleConverter implements Converter {
    private static final Logger log = Logger.getLogger(RoleConverter.class.getName());

    @Override
    public Object getAsObject(FacesContext ctx, UIComponent component,
                              String value) {
        // This will return the actual object representation
        Tblroles c = new Tblroles();
        try {
            c  =(Tblroles) db.dbTransactions.getObjectsByProperty(Tblroles.class.getCanonicalName(),"roleDesc", (String) value).get(0);
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
        if(o!=null && o instanceof Tblroles) {
            Tblroles role = (Tblroles) o;
            if (role.getRoleDesc() != null) {
                return role.getRoleDesc();
            } else {
                return null;
            }
        }else{
            return null;
        }
    }
}
