package utils;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

public class GeneralContexListener implements ServletContextListener {


    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        HttpUtils.initTrustAllClient();
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
    }
}
