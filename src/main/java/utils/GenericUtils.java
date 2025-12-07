/*
 *Δημιουργεί zip αρχεία
 */

package utils;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.LocalFileHeader;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import java.io.*;
import java.util.*;

public class GenericUtils {

    private static final Logger log = Logger.getLogger(GenericUtils.class);




    /**
     * Φίλτρο για ημερομηνίες ότάν πρέπει να επιλέξει μόνο μια ημερομηνία.
     * @param value
     * @param filter
     * @param locale
     * @return
     */
    public static boolean filterByDateColumn(Object value, Object filter, Locale locale) {

        if( filter == null || filter.equals("") ) {
            return true;
        }

        if( value == null ) {
            return false;
        }

        return DateUtils.truncatedEquals((Date) filter, (Date) value, Calendar.DATE);
    }


    /**
     * Για να προσθέτουμε message στο FacesContec
     * FacesMessage.SEVERITY_INFO
     * FacesMessage.SEVERITY_WARN
     * FacesMessage.SEVERITY_ERROR
     *
     * @param severity
     * @param summary
     */
    public static void addMessage(FacesMessage.Severity severity, String summary, String details) {
        FacesMessage message = new FacesMessage(severity, summary, details);
        FacesContext.getCurrentInstance().addMessage(null, message);
    }


    /**
     * Αλλάζει την ΕΣΣΟ και την επιστρέφει με την σωστή μορφή
     * πχ 20201 --> 20Α
     * @param esso
     * @return
     */
    public static String formatEsso(Integer esso){

        String essoString = esso.toString();
        String lastDigit = essoString.substring(4,5);
        String essoYear =  essoString.substring(2,4);

        if(lastDigit.equals("1")) {
            return essoYear + "Α";
        }else if (lastDigit.equals("2")){
            return essoYear + "Β";
        }else if (lastDigit.equals("3")){
            return essoYear + "Γ";
        }else if (lastDigit.equals("4")){
            return essoYear + "Δ";
        }else if (lastDigit.equals("5")){
            return essoYear + "Ε";
        }else if (lastDigit.equals("6")){
            return essoYear + "ΣΤ";
        }else {
            return null;
        }
    }


    /**
     * Αλλάζει την ΕΣΣΟ και την επιστρέφει με την σωστή μορφή
     * πχ 20201 --> 2020Α
     * @param esso
     * @return
     */
    public static String formatEssoFull(Integer esso){

        String essoString = esso.toString();
        String lastDigit = essoString.substring(4,5);
        String essoYear =  essoString.substring(0,4);

        if(lastDigit.equals("1")) {
            return essoYear + "Α";
        }else if (lastDigit.equals("2")){
            return essoYear + "Β";
        }else if (lastDigit.equals("3")){
            return essoYear + "Γ";
        }else if (lastDigit.equals("4")){
            return essoYear + "Δ";
        }else if (lastDigit.equals("5")){
            return essoYear + "Ε";
        }else if (lastDigit.equals("6")){
            return essoYear + "ΣΤ";
        }else {
            return null;
        }
    }



    public static void zipFilesWithCode(Map<String, String> dataMap, String password, OutputStream outputStream) throws IOException
    {


        ZipParameters zipParams = new ZipParameters();
        zipParams.setCompressionMethod(CompressionMethod.DEFLATE);
        zipParams.setCompressionLevel(CompressionLevel.NORMAL);
        zipParams.setEncryptFiles(true);
        zipParams.setEncryptionMethod(net.lingala.zip4j.model.enums.EncryptionMethod.ZIP_STANDARD);

        net.lingala.zip4j.io.outputstream.ZipOutputStream zos = new net.lingala.zip4j.io.outputstream.ZipOutputStream(outputStream, password.toCharArray());

        dataMap.forEach( (key,value) -> {

            try
            {
                zipParams.setFileNameInZip(key);
                zos.putNextEntry(zipParams);

                OutputStreamWriter osw =  new OutputStreamWriter(zos, "UTF-8");
                osw.write(value);
                osw.flush();
                zos.closeEntry();

            } catch (IOException e) {
                e.printStackTrace();
            }



        });

        zos.close();

    }


//    public static Map<String, String>  unlockZipWithCode(org.primefaces.model.UploadedFile uploadedFile, String password ) {
//        //KEY: Filename - VALUE: OutputStream
//        Map<String, OutputStream> inMemoryExtractedFiles = new HashMap<>();
//
//        //KEY: Filename - VALUE: OutputStream
//        Map<String, String> inMemoryFiles = new HashMap<>();
//
//        try
//        {
//            LocalFileHeader localFileHeader;
//
//            net.lingala.zip4j.io.inputstream.ZipInputStream zis = new net.lingala.zip4j.io.inputstream.ZipInputStream(uploadedFile.getInputstream(), password.toCharArray());
//            localFileHeader =  zis.getNextEntry();
//
//            while (localFileHeader != null) {
//                String fileName = localFileHeader.getFileName();
//                StringBuilder sb = new StringBuilder();
//
//                InputStreamReader is = new InputStreamReader(zis,"UTF-8");
//
//                BufferedReader br = new BufferedReader(is);
//                String read = br.readLine();
//
//                while (read != null) {
//                    sb.append(read);
//                    read = br.readLine();
//                }
//                inMemoryFiles.put(fileName, sb.toString());
//
//                localFileHeader = zis.getNextEntry();
//
//            }
//
//            zis.close();
//
//        } catch (ZipException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        return inMemoryFiles;
//
//
//    }



    






}
