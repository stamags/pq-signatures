/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package db;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Calendar;

/**
 * Υπολογίζει την διαφορά σε μήνες ανάμεσα σε δύο ημερομηνίες της μορφής
 * YYYY-MM-DD
 *
 * @author Vayos Bakirtzoglou <vmpakirtzoglou@gmail.com>
 */
public class TimeUtils {

    public static Calendar dateFrom;
    public static Calendar dateTo;
    public static double months;

    public static double monthsBetween(Date fromDate, Date toDate) {
        /**
         * The String[] from is a String array which contains day of month in
         * from[2], month of the year in from[1] and year in from[0]        
        *
         */
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        String fromDateString = formatter.format(fromDate);
        
        
        String[] fromString = fromDateString.split("-");
        String[] toString;

        if (toDate == null) {
            toString = ("2012-12-31").split("-");
//            toDate = (Calendar.getInstance()).getTime();
//            String toDateString = formatter.format(toDate);
//            toString = toDateString.split("-");
        } else {
            String toDateString = formatter.format(toDate);
            toString = toDateString.split("-");
            // An eimaste stin 30 hmera twn minwn Apr, Jun, Sep, Nov tote 8etei tin hmera isi me 31
            if ((toString[1].equals("04") || toString[1].equals("06") || toString[1].equals("09") || toString[1].equals("11")) && toString[2].equals("30")) {
                toString[2] = "31";
            }
            // An eimaste se disekto etos => (year modulo 4 == 0)
            // 8etei thn 29 hmera isi me 31
            if (Double.parseDouble(toString[0]) % 4 == 0 && toString[2].equals("29")) {
                toString[2] = "31";
                // An den eimaste se disekto etos tote 8etei tin 28 hmera isi me 31    
            } else if (Double.parseDouble(toString[0]) % 4 != 0 && toString[2].equals("28")) {
                toString[2] = "31";
            }
        }

        double yearFrom = Double.parseDouble(fromString[0]);
        double monFrom = Double.parseDouble(fromString[1]);
        double dayFrom = Double.parseDouble(fromString[2]);

        double yearTo = Double.parseDouble(toString[0]);
        double monTo = Double.parseDouble(toString[1]);
        double dayTo = Double.parseDouble(toString[2]) + 1;

        months = (12 * (yearTo - yearFrom) + (monTo - monFrom) + (dayTo - dayFrom) / 31);
        return months;
    }
}
