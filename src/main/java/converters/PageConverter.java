package converters;

import model.Tblpages;

import jakarta.annotation.ManagedBean;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;
import java.util.logging.Logger;


/**
 * Created by tsotzolas
 */
@ManagedBean
@FacesConverter(value = "pageConverter")
public class PageConverter implements Converter {
    private static final Logger log = Logger.getLogger(PageConverter.class.getName());

    @Override
    public Object getAsObject(FacesContext ctx, UIComponent component,
                              String value) {
        // This will return the actual object representation
        Tblpages c = new Tblpages();
        try {
            c  =(Tblpages) db.dbTransactions.getObjectsByProperty(Tblpages.class.getCanonicalName(),"pageName", (String) value).get(0);
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
        if(o!=null && o instanceof Tblpages) {
            Tblpages page = (Tblpages) o;
            if (page.getPageName() != null) {
                return page.getPageName();
            } else {
                return null;
            }
        }else{
            return null;
        }

    }


}
