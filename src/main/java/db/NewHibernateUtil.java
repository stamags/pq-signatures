///*
// * To change this template, choose Tools | Templates
// * and open the template in the editor.
// */
//package db;
//
//import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
//import org.hibernate.SessionFactory;
//import org.hibernate.cfg.Configuration;
//import org.hibernate.cfg.DefaultNamingStrategy;
//import org.hibernate.engine.spi.SessionFactoryImplementor;
//import org.hibernate.engine.spi.SessionImplementor;
//import org.hibernate.service.ServiceRegistry;
//import java.sql.Connection;
//import java.sql.SQLException;
//
///**
// * Hibernate Utility class with a convenient method to get Session Factory
// * object.
// *
// * @author Almalis Nikolaos <nikosalmalis@gmail.com>
// */
//public class NewHibernateUtil {
//
//    private static SessionFactory sessionFactory;
//
//    public static SessionFactory getSessionFactory() {
//        if (sessionFactory == null) {
//            // loads configuration and mappings
//            Configuration configuration = new Configuration().configure("/hibernate,cfg.xml");
////            configuration.setNamingStrategy(DefaultNamingStrategy.INSTANCE);
////            ServiceRegistry serviceRegistry
////                    = new StandardServiceRegistryBuilder()
////                    .applySettings(configuration.getProperties()).build();
////            ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
////                    .applySettings(configuration.getProperties()).build();
//           sessionFactory = configuration.configure().buildSessionFactory();
//            // builds a session factory from the service registry
////            sessionFactory = configuration.buildSessionFactory(serviceRegistry);
//        }
//
//        return sessionFactory;
//
//    }
//
//    public static Connection getConnection() throws SQLException {
//        SessionImplementor sessionImplementor;
//        sessionImplementor = (SessionImplementor) sessionFactory.openSession();
//        return sessionImplementor.getJdbcConnectionAccess().obtainConnection();
//    }
//}