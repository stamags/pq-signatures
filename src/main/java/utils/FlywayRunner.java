package utils;


import org.apache.commons.lang3.SystemUtils;
import org.flywaydb.core.Flyway;

import javax.servlet.ServletContextListener;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;


public class FlywayRunner implements ServletContextListener {

    public static void flyway() throws IOException {
//        String kenPropertiesfilePath = "";
//        if (SystemUtils.IS_OS_LINUX) {
//            kenPropertiesfilePath = "/ken.properties";
//        } else {
//            kenPropertiesfilePath = "\\ken.properties";
//        }
//
//
//        String jbossHomePath = System.getProperty("jboss.home.dir") + kenPropertiesfilePath;
////        String jbossHomePath = "/home/tsotzo/wildfly-18.0.0.Final" + kenPropertiesfilePath;
//
//
//        FileInputStream in = new FileInputStream(jbossHomePath);
//        Properties prop = new Properties();
//        prop.load(in);
//
//
//        String url = prop.getProperty("flyway.url");
//        String user = prop.getProperty("flyway.user");
//        String pass = prop.getProperty("flyway.password");
//        String db = prop.getProperty("flyway.schemas");
//        String location = prop.getProperty("flyway.locations");
//
//        // Create the Flyway instance and point it to the database
//        Flyway flyway = Flyway.configure().dataSource(url, user, pass).load();
//
//        // Start the migration
////        flyway.baseline();
//        flyway.repair();
//        flyway.migrate();
    }

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        try {
            flyway();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {

    }
}
