/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package utils;

//import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Meletiadis Vasilis <kurtz.pentagon@gmail.com>
 */
public class StringUtils {
    
    public static String getLatinFromGreek(String input){
        String result;
        input=input.toLowerCase();
        
        
        String[] REGEXdouble ={"αι","αυ([θκξπσςτφχψ]|\\s|$)","αυ","οι","ου","ει",
            "ευ([θκξπσςτφχψ]|\\s|$)","ευ","(^|\\s)μπ","μπ(\\s|$)","μπ","ντ","τσ",
            "τζ","γγ","γκ","ηυ([θκξπσςτφχψ]|\\s|$)","ηυ","θ","χ","ψ"    
        };
        String[] REPLACEdouble = {"ai","af$1","av","oi","ou","ei","ef$1","ev","$1b",
            "b$1","mp","nt","ts","tz","ng","gk","if$1","iy","th","ch","ps"
            };
        
        Pattern p=Pattern.compile(REGEXdouble[0]);
        Matcher m = p.matcher (input);
        result= m.replaceAll(REPLACEdouble[0]);
        
        
        for (int i=1;i<REGEXdouble.length;i++)
        {
           p=Pattern.compile(REGEXdouble[i]);
           m=p.matcher(result);
           result=m.replaceAll(REPLACEdouble[i]);
           
        }
        
        
        String REGEX ="αάβγδεέζηήιίΐϊκλμνξοόπρστυύϋΰφωώς";
        String REPLACE = "aabgdeeziiiiiiklmnxooprstyyiifoos";
        
         p=Pattern.compile(REGEX.substring(0, 1));
         m = p.matcher (result);
        result= m.replaceAll(REPLACE.substring(0, 1));
        
        
        for (int i=1;i<REGEX.length();i++)
        {
           p=Pattern.compile(REGEX.substring(i, i+1));
           m=p.matcher(result);
           result=m.replaceAll(REPLACE.substring(i, i+1));
           
        }
        
        
        
        return result;
    }

    /**
     * Μέθοδος μετατροπής ενος ελληνικού string σε κεφαλαία
     * αφαιρώντας τόνους, διαλυτικά
     *
     * @param str
     * @return
     */
    public static String toUpper(String str)
    {
        String upperLoc = str.toUpperCase(new Locale("el"));
        String upperNormalized = Normalizer.normalize(upperLoc, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCOMBINING_DIACRITICAL_MARKS}+");

        return pattern.matcher(upperNormalized).replaceAll("");
    }


    /**
     * Παίρνει μια λίστα απο String και την κάνει ένα string με ','
     * @param list
     * @return
     */
    public static String listToArray(List<String> list) {

        String r = "";
        if (list.size() > 0) {
            r = org.apache.commons.lang3.StringUtils.join(list, ",");
        }


        return r;
    }

    /**
     * Παίρνει μια λίστα απο String και την κάνει ένα string με ','
     * αλλά για να το χρησιμοποιήσουμε σαν string μέσα σε ένα ερώτημα/
     * @param list
     * @return
     */
    public static String listToArrayForString(List<String> list) {

        String r = " '";
        if (list.size() > 0) {
            r = r + org.apache.commons.lang3.StringUtils.join(list, "','");
        }
        r = r + "'";


        return r;
    }


    /**
     * Μέθοδος που μετατρέπει ένα inputstream σε BASE64 string
     * @param inputStream
     * @return base64 String
     */
    public static  String inputStreamToBase64String(InputStream inputStream){

        String encodedString = "";
        try {
//            InputStream is = inputStream;
            byte[] fileContent = IOUtils.toByteArray(inputStream);
////            fileContent = IOUtils.toByteArray(is);
//            encodedString = Base64.encodeBase64String(fileContent);
            encodedString = Base64.getEncoder().encodeToString(fileContent);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return encodedString;
    }


    /**
     * Μέθοδος που μετατρέπει ένα byte[] σε BASE64 string
     * @param
     * @return base64 String
     */
    public static  String bytesToBase64String(byte[] input){

        String encodedString = "";
        try {
//            InputStream is = inputStream;
            encodedString = Base64.getEncoder().encodeToString(input);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return encodedString;
    }

}
