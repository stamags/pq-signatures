package utils;

import org.apache.commons.lang3.SystemUtils;
import org.apache.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**
 * Διαβάζουμε το PropertyFile
 */
public class PropertyFile {

    private static final Logger log = Logger.getLogger(PropertyFile.class);

    public static String esso() throws IOException {

        String kenPropertiesfilePath = "";
        if (SystemUtils.IS_OS_LINUX) {
            kenPropertiesfilePath = "/ken.properties";
        }else{
            kenPropertiesfilePath = "\\ken.properties";
        }

        //Μερικοί Ν/Σ χάνουν την ΕΣΣΟ.Γι αυτό αν κάποιος δεν την έχει την σεταρουμε εδώ
        // Παίρνουμε την ΕΣΣΟ
        String jbossHomePath = System.getProperty("jboss.home.dir") + kenPropertiesfilePath;
        FileInputStream in = new FileInputStream(jbossHomePath);
        Properties prop = new Properties();
        prop.load(in);

        String esso = prop.getProperty("currentEsso");
        return esso;
    }

    public static String ken() throws IOException {

        String kenPropertiesfilePath = "";
        if (SystemUtils.IS_OS_LINUX) {
            kenPropertiesfilePath = "/ken.properties";
        }else{
            kenPropertiesfilePath = "\\ken.properties";
        }

        //Μερικοί Ν/Σ χάνουν την ΕΣΣΟ.Γι αυτό αν κάποιος δεν την έχει την σεταρουμε εδώ
        // Παίρνουμε την ΕΣΣΟ
        String jbossHomePath = System.getProperty("jboss.home.dir") + kenPropertiesfilePath;
        FileInputStream in = new FileInputStream(jbossHomePath);
        Properties prop = new Properties();
        prop.load(in);

        String ken = prop.getProperty("ken");
        return ken;
    }

    public static Logger getLog() {
        return log;
    }


}
