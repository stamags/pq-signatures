package utils;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.FacesValidator;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;
import java.util.regex.Pattern;

/**
* Created by Γεώργιος on 2/1/2016.
*/
@FacesValidator("emailValidator")
public class EmailValidator implements Validator {

    private static final Pattern PATTERN = Pattern.compile("([^.@]+)(\\.[^.@]+)*@([^.@]+\\.)+([^.@]+)");


    public void validate(FacesContext context, UIComponent component, Object value) throws ValidatorException {
        if (value == null || ((String) value).isEmpty()) {
            return; // Let required="true" handle.
        }

        if (!PATTERN.matcher((String) value).matches()) {
            String summary = context.getApplication().evaluateExpressionGet(context, "Εσφαλμένο email", String.class);
            String detail = context.getApplication().evaluateExpressionGet(context, "το email δεν έχει τη σωστή μορφή", String.class);

            throw new ValidatorException(new FacesMessage(FacesMessage.SEVERITY_ERROR, summary, detail));
        }
    }

}
