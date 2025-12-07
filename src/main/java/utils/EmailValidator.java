package utils;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.validator.FacesValidator;
import jakarta.faces.validator.Validator;
import jakarta.faces.validator.ValidatorException;
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
