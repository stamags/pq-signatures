package db;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.persistence.*;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.query.NativeQuery;


/**
 * Provides Database Interaction Layer for the Entire Project. dbTransactions
 * should be Statically called by any invoker e.g. dbTransactions.addUser(args)
 *
 * @author Panagiotis Gouvas
 */
public class dbTransactions {

    public static final int ORDER_ASC = 0;
    public static final int ORDER_DESC = 1;
    public static final String GT = ">";
    public static final String GE = ">=";
    public static final String LT = "<";
    public static final String LE = "<=";
    public static final String NE = "!=";
    public static final String EQ = "=";
    public static final String IS_NULL = "IS NULL";
    public static final String IS_NOT_NULL = "IS NOT NULL";
    public static final String LIKE = "LIKE";
    public static final String NOT_LIKE = "NOT LIKE";
    public static final String I_LIKE = "ILIKE";
    private static final Logger log = Logger.getLogger(dbTransactions.class.getName());

    public static <T> T getObjectById(Class<T> clazz, Object id) {
        return (T) getObjectById(clazz.getCanonicalName(), id);
    }

    /**
     * Method getObjectById(String id) loads the appropriate hibernate Object
     * from the database.
     *
     * @param classname The classname of the loaded class e.g. model.User
     * @param id        The id as mapped in the database
     * @return The hibernate Object that is loaded or null
     */
    public static Object getObjectById(String classname, Object id) {

        //Hibernate Initialisation
        EntityManager entityManager = null;
        //Model Initialisation
        Object obj = null;
        try {
            entityManager = JPAUtil.getEntityManagerFactory().createEntityManager();
            entityManager.getTransaction().begin();

            if (id instanceof String) {
                obj = entityManager.find(Class.forName(classname), (String) id);

            } else if (id instanceof Integer) {
                obj = entityManager.find(Class.forName(classname), (Integer) id);
            } else if (id instanceof Short) {
                obj = entityManager.find(Class.forName(classname), (Short) id);
            } else if (id instanceof Date) {
                obj = entityManager.find(Class.forName(classname), (Date) id);
            } else if (id instanceof Long) {
                obj = entityManager.find(Class.forName(classname), (Long) id);
            } else if (id instanceof Byte) {
                obj = entityManager.find(Class.forName(classname), (Byte) id);
            }

            entityManager.getTransaction().commit();

        } catch (Exception ex) {
            log.error("dbTransactions.getObjectById(" + id + "): An error occurred during the transaction. Attempting to RollBack", ex);
            try {
                entityManager.getTransaction().rollback();
            } catch (Exception re) {
                log.error("dbTransactions.getObjectById(" + id + "): An error occurred attempting to roll back the transaction.", re);
            }
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }
        log.info("dbTransactions.getObjectById(" + id + ") returns Object:" + ((obj != null) ? obj.getClass() : null));

        return obj;
    }//EoM getObjectById

    /**
     * Wrapper μέθοδος για την ανάκτηση αντικειμένων βάσει συγκεκριμένων πεδίων.
     *
     * @param classname
     * @param properties
     * @return
     */
    public static List<Object> getObjectsByProperties(String classname, Map<String, Object> properties) {
        return getObjectsByProperties(classname, properties, null, null, null);
    }//EoM getObjectsByProperties

    /**
     * Wrapper μέθοδος για την ανάκτηση αντικειμένων βάσει συγκεκριμένων πεδίων
     * και χρήση σελιδοποίησης.
     *
     * @param classname
     * @param properties
     * @param page
     * @param pagesize
     * @return
     */

    /**
     * use this method instead of returning the Connection reference
     *
     * @param connectionConsumer
     * @param <T>
     * @return
     */
    public static <T> T useConnectionFor(Function<Connection,T> connectionConsumer) {
        return executeQueryCommand(entityManager -> {
            SessionImplementor sessionImplementor = (SessionImplementor) entityManager;
            try (Connection con = sessionImplementor.getJdbcConnectionAccess().obtainConnection();) {
                return connectionConsumer.apply(con);
            } catch (SQLException sql) {
                throw new IllegalStateException(sql);
            }
        });
    }
    public static void useConnection(Consumer<Connection> con){
        useConnectionFor(a->{
            con.accept(a);
            return true;
        });
    }

    /**
     * no transaction
     *
     * @param command
     * @param <T>
     * @return
     */
    public static <T> T executeQueryCommand(Function<EntityManager,T> command){
        return executeGenericCommand(JPAUtil.getEntityManagerFactory(), command,false);
    }

    private static <T> T executeGenericCommand(EntityManagerFactory entityManagerFactory, Function<EntityManager,T> command, boolean isTransaction){
        return executeGenericCommand(
                entityManagerFactory,
                command,
                isTransaction,
                ex->log.error("Error in hibernate transaction: " + ex.getLocalizedMessage()));
    }

    private static <T> T executeGenericCommand(EntityManagerFactory entityManagerFactory, Function<EntityManager,T> command,boolean isTransaction,Consumer<Exception> errExc){
        return executeGenericCommand(
                entityManagerFactory,
                command,
                isTransaction,
                errExc,
                ex->log.error("An error occurred attempting to roll back the transaction.",ex));
    }

    private static <T> T executeGenericCommand(          EntityManagerFactory entityManagerFactory,
                                                        Function<EntityManager,T> command,
                                                        boolean isTransaction,
                                                        Consumer<Exception> errExc,
                                                        Consumer<Exception>rollExc) {
        EntityManager entityManager = null;
        EntityTransaction transaction = null;
        try {
            entityManager = entityManagerFactory.createEntityManager();
            if(isTransaction) {
                transaction = entityManager.getTransaction();
                transaction.begin();
            }

            T t = command.apply(entityManager);
            if(isTransaction)
                transaction.commit();
            return t;
        } catch (HibernateException ex) {
            errExc.accept(ex);
            try {
                // Roll back the transaction
                if (isTransaction && transaction != null) {
                    transaction.rollback();
                }

                // Re-throw exception
                throw ex;
            } catch (Exception re) {
                rollExc.accept(re);
//                log.error("An error occurred attempting to roll back the transaction.", re);
                throw re;
            }
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }
    }

    public static List<Object> getObjectsByProperties(String classname, Map<String, Object> properties, Integer page, Integer pagesize) {
        return getObjectsByProperties(classname, properties, page, pagesize, null);
    }//EoM getObjectsByProperties

    /**
     * Wrapper μέθοδος για την ανάκτηση αντικειμένων βάσει συγκεκριμένων πεδίων,
     * με χρήση σελιδοποίησης και ταξινόμησης.
     *
     * @param classname
     * @param properties
     * @param page
     * @param pagesize
     * @param sortproperties
     * @return
     */
    public static List<Object> getObjectsByProperties(String classname, Map<String, Object> properties,
                                                      Integer page, Integer pagesize, Map<String, Integer> sortproperties) {

        return getObjectsByProperties(classname, properties, null, page, pagesize, sortproperties);
    }//EoM getObjectsByProperties

    /**
     * @param procedureName
     * @param procedureVariables
     */
//    public static List<Object> executeProcedure(String procedureName, List<Object> procedureVariables) {
//
//        EntityManager entityManager = null;
//        List<Object> retlist = null;
//
//
//        try {
//            entityManager = JPAUtil.getEntityManagerFactory().createEntityManager();
//
//            // Dynamic stored procedure definition.
//            StoredProcedureQuery addProcedure = entityManager.createStoredProcedureQuery(procedureName);
//            if (procedureVariables.size() > 0) {
//
//                int i;
//                for (i = 0; i < procedureVariables.size(); i++) {
//
//
//                    if (procedureVariables.get(i) != null) {
//                        if (procedureVariables.get(i) instanceof String) {
//                            addProcedure.registerStoredProcedureParameter(i, String.class, ParameterMode.IN);
//
//                        } else if (procedureVariables.get(i) instanceof Integer) {
//                            addProcedure.registerStoredProcedureParameter(i, Integer.class, ParameterMode.IN);
//                        } else if (procedureVariables.get(i) instanceof Date) {
//                            addProcedure.registerStoredProcedureParameter(i, Date.class, ParameterMode.IN);
//                        } else if (procedureVariables.get(i) instanceof Boolean) {
//                            addProcedure.registerStoredProcedureParameter(i, Boolean.class, ParameterMode.IN);
//                        } else if (procedureVariables.get(i) instanceof Double) {
//                            addProcedure.registerStoredProcedureParameter(i, Double.class, ParameterMode.IN);
//                        } else if (procedureVariables.get(i) instanceof Long) {
//                            addProcedure.registerStoredProcedureParameter(i, Long.class, ParameterMode.IN);
//                        } else if (procedureVariables.get(i) instanceof BigDecimal) {
//                            addProcedure.registerStoredProcedureParameter(i, BigDecimal.class, ParameterMode.IN);
//                        }
//                    }
//
//                    // Setting stored procedure parameters.
//                    if (procedureVariables.get(i) instanceof Date) {
//                        addProcedure.setParameter(i , (Date) procedureVariables.get(i), TemporalType.DATE);
//                    } else {
//                        addProcedure.setParameter(i , procedureVariables.get(i));
//                    }
//                }
//            }
//
//            retlist = (List<Object>) addProcedure.getResultList();
//            entityManager.getTransaction().commit();
//        } catch (Exception ex) {
//            log.error("dbTransactions.getObjectsByProperty(): An error occurred during the transaction. Attempting to RollBack", ex);
//            try {
//                entityManager.getTransaction().rollback();
//            } catch (Exception re) {
//                log.error("An error occured in executeProcedure");
//                ex.printStackTrace();
//            }
//        } finally {
//            if (entityManager != null) {
//                entityManager.close();
//            }
//        }
//        return retlist;
//    }//EoM executeProcedure

    public static List<Object> getObjectsByProperties(String
                                                              classname, Map<String, Object> properties, Map<String, Object[]> advancedProperties,
                                                      Integer page, Integer pagesize, Map<String, Integer> sortproperties) {

        // construct query
        int counter = 1;
        String query = "From " + classname + " c ";

        // add properties
        if (properties != null) {
            query += " where ";
            for (String propertyKey : properties.keySet()) {
                if (counter == 1) {
                    query += "upper(" + propertyKey + ") like upper(:param" + counter++ + ") ";
                } else {
                    query += " AND upper(" + propertyKey + ") like upper(:param" + counter++ + ") ";
                }
            }
        }

        // add advanced properties
        if (advancedProperties != null) {
            if (Objects.isNull(properties)) query += " where ";
            for (String propertyKey : advancedProperties.keySet()) {
                if (counter == 1) {
                    query += propertyKey + " " + advancedProperties.get(propertyKey)[0] + " :param" + counter++ + " ";
                } else {
                    query += " AND " + propertyKey + " " + advancedProperties.get(propertyKey)[0] + " :param" + counter++ + " ";
                }
            }
        }

        // add sorting properties
        if (sortproperties != null) {
            counter = 1;
            for (String sortKey : sortproperties.keySet()) {
                if (counter == 1) {
                    query += " order by " + sortKey + " " + ((sortproperties.get(sortKey) == 0) ? "ASC" : "DESC");
                } else {
                    query += " ," + sortKey + " " + ((sortproperties.get(sortKey) == 0) ? "ASC" : "DESC");
                }
                counter++;
            }
        }

        EntityManager entityManager = null;
        List<Object> retlist = null;

        try {
            entityManager = JPAUtil.getEntityManagerFactory().createEntityManager();
            EntityTransaction tx = entityManager.getTransaction();
            tx.begin();
            Query q = entityManager.createQuery(query);

            log.info("Executing query " + query);
            counter = 1;
            if (!Objects.isNull(properties)) {
                for (Object propertyValue : properties.values()) {
                    if (propertyValue instanceof String) {
                        q.setParameter("param" + counter++, "%" + propertyValue + "%");
                    } else if (propertyValue instanceof Integer) {
                        q.setParameter("param" + counter++, propertyValue);
                    } else if (propertyValue instanceof Short) {
                        q.setParameter("param" + counter++, propertyValue);
                    } else if (propertyValue instanceof Date) {
                        q.setParameter("param" + counter++, propertyValue);
                    } else if (propertyValue instanceof Long) {
                        q.setParameter("param" + counter++, propertyValue);
                    }else if (propertyValue instanceof Boolean) {
                        q.setParameter("param" + counter++, propertyValue);
                    }
                }
            }
            if (!Objects.isNull(advancedProperties)) {
                if (advancedProperties != null) {
                    for (Object[] o : advancedProperties.values()) {
                        if (o[1] instanceof String) {
                            q.setParameter("param" + counter++, "%" + (String) o[1] + "%");
                        } else if (o[1] instanceof Integer) {
                            q.setParameter("param" + counter++, (Integer) o[1]);
                        } else if (o[1] instanceof Short) {
                            q.setParameter("param" + counter++, (Short) o[1]);
                        } else if (o[1] instanceof Date) {
                            q.setParameter("param" + counter++, (Date) o[1]);
                        }
                        else if (o[1] instanceof Boolean) {
                            q.setParameter("param" + counter++, (Boolean) o[1]);
                        }
                    }
                }
            }

            // enable pagination only if values are set
            if (page != null && pagesize != null) {
                q.setFirstResult((page - 1) * pagesize);
                q.setMaxResults(pagesize);
            }
            retlist = q.getResultList();
            tx.commit();
        } catch (Exception ex) {
            log.error("dbTransactions.getObjectsByProperty(): An error occurred during the transaction. Attempting to RollBack", ex);
            try {
                entityManager.getTransaction().rollback();
            } catch (Exception re) {
                log.error("An error occured in getObjectsByProperties with classname " + classname
                        + " and properties " + properties);
                ex.printStackTrace();
            }
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }
        return retlist;
    }//EoM executeProcedure

    /**
     * Counts all objects against String and Integer property values.
     *
     * @param classname
     * @return
     */
    public static Integer countAllObjectsByProperties(String classname, Map<String, Object> properties) {
        return countAllObjectsByProperties(classname, properties, null);
    }//EoM countAllObjectsByProperties

    /**
     * Counts all objects against String and Integer property values.
     *
     * @param classname
     * @param properties
     * @param advancedProperties
     * @return
     */
    public static Integer countAllObjectsByProperties(String classname, Map<String, Object> properties, Map<String, Object[]> advancedProperties) {

        // construct query
        String query = "Select count(*) from " + classname + " c ";
        int counter = 1;
        if (properties != null) {
            query += " where ";
            for (String propertyKey : properties.keySet()) {
                if (counter == 1) {
                    query += "upper(" + propertyKey +
                            ") like upper(:param" + counter++ + ") ";
                } else {
                    query += " AND upper(" + propertyKey + ") like upper(:param" + counter++ + ") ";
                }
            }
        }

        // add advanced properties
        if (advancedProperties != null) {
            if (Objects.isNull(properties)) query += " where ";
            for (String propertyKey : advancedProperties.keySet()) {
                if (counter == 1) {
                    query += propertyKey + " " + advancedProperties.get(propertyKey)[0] + " :param" + counter++ + " ";
                } else {
                    query += " AND " + propertyKey + " " + advancedProperties.get(propertyKey)[0] + " :param" + counter++ + " ";
                }
            }
        }

        EntityManager entityManager = null;
        List<Object> retlist = null;
        Integer num = null;
        Query q = null;

        try {
            entityManager = JPAUtil.getEntityManagerFactory().createEntityManager();
            entityManager.getTransaction().begin();
            q = entityManager.createQuery(query);


            counter = 1;
            if (!Objects.isNull(properties)) {
                for (Object propertyValue : properties.values()) {
                    if (propertyValue instanceof String) {
                        q.setParameter("param" + counter++, "%" + propertyValue + "%");
                    } else if (propertyValue instanceof Integer) {
                        q.setParameter("param" + counter++, propertyValue);
                    } else if (propertyValue instanceof Short) {
                        q.setParameter("param" + counter++, propertyValue);
                    } else if (propertyValue instanceof Date) {
                        q.setParameter("param" + counter++, propertyValue);
                    } else if (propertyValue instanceof Long) {
                        q.setParameter("param" + counter++, propertyValue);
                    }else if (propertyValue instanceof Boolean) {
                        q.setParameter("param" + counter++, propertyValue);
                    }
                }
            }

            if (!Objects.isNull(advancedProperties)) {
                if (advancedProperties != null) {
                    for (Object[] o : advancedProperties.values()) {
                        if (o[1] instanceof String) {
                            q.setParameter("param" + counter++, "%" + (String) o[1] + "%");
                        } else if (o[1] instanceof Integer) {
                            q.setParameter("param" + counter++, (Integer) o[1]);
                        } else if (o[1] instanceof Short) {
                            q.setParameter("param" + counter++, (Short) o[1]);
                        } else if (o[1] instanceof Date) {
                            q.setParameter("param" + counter++, (Date) o[1]);
                        }else if (o[1] instanceof Boolean) {
                            q.setParameter("param" + counter++, (Boolean) o[1]);
                        }
                    }
                }
            }

            num = ((Long) q.getSingleResult()).intValue();
            entityManager.getTransaction().commit();
        } catch (Exception ex) {
            log.error("dbTransactions.getObjectsByProperties(" + classname + "): An error occurred during the transaction. Attempting to RollBack", ex);
            try {
                entityManager.getTransaction().rollback();
            } catch (Exception re) {
            }
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }

        return num;
    }//EoM countAllObjectsByProperties

    public static List<Object> getObjectsBySqlQuery(Class classname, String query, List<Object> params, Integer
            page, Integer pagesize) {

        Query q;
        List<Object> obj = null;
        EntityManager entityManager = null;
        try {
            entityManager = JPAUtil.getEntityManagerFactory().createEntityManager();
            entityManager.getTransaction().begin();

            String finalQuery = "SELECT e.* " + query;
            if (classname == null) {
                finalQuery = "SELECT * " + query;
            }

            // Applying pagination on query level. Otherwise, without HQL, the pagination is applied in the acquired resultset.
            if (page != null && pagesize != null) {
                //For Oracle
//                finalQuery = "SELECT k.* FROM (SELECT row_.*, rownum rownum_ FROM ( " + finalQuery + " ) row_) k WHERE rownum_ > " + ((page - 1) * pagesize) + " AND rownum_ <= " + (((page - 1) * pagesize) + pagesize);

                // For Mysql
                finalQuery = "SELECT k.* FROM (SELECT row_.*,@rownum\\:= @rownum + 1 as rownum_ FROM ( " + finalQuery + " ) row_ , (SELECT @rownum\\:= 0) as i) k WHERE rownum_ > " + ((page - 1) * pagesize) + " AND rownum_ <= " + (((page - 1) * pagesize) + pagesize);
            }

            if (classname != null) {
                q = entityManager.createNativeQuery(finalQuery, classname);
            } else {
                q = entityManager.createNativeQuery(finalQuery);
            }

            if (params != null) {
                int counter = 1;
                for (Object o : params) {
                    if (o instanceof String) {
                        q.setParameter("param" + counter++, (String) o);
                    } else if (o instanceof Integer) {
                        q.setParameter("param" + counter++, o);
                    } else if (o instanceof Date) {
                        q.setParameter("param" + counter++, (Date) o);
                    } else if (o instanceof Boolean) {
                        q.setParameter("param" + counter++, (Boolean) o);
                    }

                }
            }

            obj = q.getResultList();
            entityManager.getTransaction().commit();
        } catch (Exception ex) {
            log.error("Error in getObjectsByCustomQuery: " + ex.getLocalizedMessage());
            try {
                entityManager.getTransaction().rollback();
            } catch (Exception re) {
                log.error("An error occurred attempting to roll back the transaction.", re);
            }
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }
        return obj;

    }//EoM getObjectsBySqlQuery

    /**
     * Παίρνει ακριβώς το query με distinct, όμως πρέπει να φτιαχτεί κατάλληλη
     * κλάση για την διαχείρηη των αποτελεσμάτων sto hbm
     *
     * @param classname
     * @param query
     * @return
     */
    public static List<Object> getObjectsBySqlQueryDistinct(Class classname, String query, List<Object> params, Integer
            page, Integer pagesize) {

        Query q;
        List<Object> obj = null;
        EntityManager entityManager = null;
        try {
            entityManager = JPAUtil.getEntityManagerFactory().createEntityManager();
            entityManager.getTransaction().begin();

            String finalQuery = "SELECT DISTINCT e.* " + query;
            if (classname == null) {
                finalQuery = "SELECT DISTINCT * " + query;
            }

            // Applying pagination on query level. Otherwise, without HQL, the pagination is applied in the acquired resultset.
            if (page != null && pagesize != null) {
                //For Oracle
//                finalQuery = "SELECT k.* FROM (SELECT DISTINCT row_.*, rownum rownum_ FROM ( " + finalQuery + " ) row_) k WHERE rownum_ > " + ((page - 1) * pagesize) + " AND rownum_ <= " + (((page - 1) * pagesize) + pagesize);

                // For Mysql
                finalQuery = "SELECT k.* FROM (SELECT DISTINCT row_.*,@rownum\\:= @rownum + 1 as rownum_ FROM ( " + finalQuery + " ) row_ , (SELECT @rownum\\:= 0) as i) k WHERE rownum_ > " + ((page - 1) * pagesize) + " AND rownum_ <= " + (((page - 1) * pagesize) + pagesize);
            }

            if (classname != null) {
                q = entityManager.createNativeQuery(finalQuery, classname);
            } else {
                q = entityManager.createNativeQuery(finalQuery);
            }

            if (params != null) {
                int counter = 1;
                for (Object o : params) {
                    if (o instanceof String) {
                        q.setParameter("param" + counter++, (String) o);
                    } else if (o instanceof Integer) {
                        q.setParameter("param" + counter++, o);
                    } else if (o instanceof Date) {
                        q.setParameter("param" + counter++, (Date) o);
                    } else if (o instanceof Boolean) {
                        q.setParameter("param" + counter++, (Boolean) o);
                    }

                }
            }

            obj = q.getResultList();
            entityManager.getTransaction().commit();
        } catch (Exception ex) {
            log.error("Error in getObjectsByCustomQuery: " + ex.getLocalizedMessage());
            try {
                entityManager.getTransaction().rollback();
            } catch (Exception re) {
                log.error("An error occurred attempting to roll back the transaction.", re);
            }
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }
        return obj;
    }

    public static List<Object> getObjectsBySqlQueryOrdered(Class classname, String query, String sortproperty,
                                                           int sorttype) {

        Query q;
        List<Object> obj = null;
        EntityManager entityManager = null;
        try {
            entityManager = JPAUtil.getEntityManagerFactory().createEntityManager();
            entityManager.getTransaction().begin();
            String finalQuery = "SELECT c.* " + query + " order by c." + sortproperty + " " + ((sorttype == 0) ? "asc" : "desc");

            if (classname != null) {
                q = entityManager.createNativeQuery(finalQuery, classname);
            } else {
                q = entityManager.createNativeQuery(finalQuery);
            }

            obj = q.getResultList();
            entityManager.getTransaction().commit();
        } catch (Exception ex) {
            log.error("Error in getObjectsByCustomQuery: " + ex.getLocalizedMessage());
            try {
                entityManager.getTransaction().rollback();
            } catch (Exception re) {
                log.error("dbTransactions.getObjectsByProperty(" + classname + "): An error occurred attempting to roll back the transaction.", re);
            }
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }
        return obj;

    }

    /**
     * @param classname
     * @param query
    //     * @param params
     * @return
     */
    public static Integer countObjectsBySqlQuery(Class classname, String query) {

        Query q;
        EntityManager entityManager = null;
        Integer obj = null;
        Object o;

        try {
            entityManager = JPAUtil.getEntityManagerFactory().createEntityManager();
            entityManager.getTransaction().begin();

            q = entityManager.createNativeQuery("SELECT count(*) " + query);

            o = q.getSingleResult();
            obj = ((BigInteger) o).intValue();
            entityManager.getTransaction().commit();
        } catch (Exception ex) {
            log.error("Error in countObjectsByCustomQuery: " + ex.getLocalizedMessage());
            try {
                entityManager.getTransaction().rollback();
            } catch (Exception re) {
                log.error("dbTransactions.getObjectsByProperty(" + classname + "): An error occurred attempting to roll back the transaction.", re);
            }
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }
        return obj;
    }


    public static List<Object> getObjectsBySqlQueryNew(String classname, String query, List<Object> params, Integer page, Integer pagesize) {

        Query q;
        List<Object> obj = null;
        EntityManager entityManager = null;

        try {
            entityManager = JPAUtil.getEntityManagerFactory().createEntityManager();
            entityManager.getTransaction().begin();
            
            String finalQuery = "SELECT * " + query;

            // Applying pagination on query level. Otherwise, without HQL, the pagination is applied in the acquired resultset.
            if (page != null && pagesize != null) {
                finalQuery = "SELECT * FROM (SELECT row_.*, rownum rownum_ FROM ( " + finalQuery + " ) row_) WHERE rownum_ > " + ((page - 1) * pagesize) + " AND rownum_ <= " + (((page - 1) * pagesize) + pagesize);
            }
            
            // Fix: Use createNativeQuery with Class object instead of addEntity
            if (classname != null) {
                Class<?> entityClass = Class.forName(classname);
                q = entityManager.createNativeQuery(finalQuery, entityClass);
            } else {
                q = entityManager.createNativeQuery(finalQuery);
            }

            if (params != null) {
                int counter = 1;
                for (Object o : params) {
                    if (o instanceof String) {
                        q.setParameter("param" + counter++, (String) o);
                    } else if (o instanceof Integer) {
                        q.setParameter("param" + counter++, o);
                    } else if (o instanceof Date) {
                        q.setParameter("param" + counter++, (Date) o);
                    }
                }
            }

            obj = q.getResultList();
            entityManager.getTransaction().commit();
        } catch (Exception ex) {
            log.error("Error in getObjectsByCustomQuery: " + ex.getLocalizedMessage());
            try {
                entityManager.getTransaction().rollback();
            } catch (Exception re) {
                log.error("An error occurred attempting to roll back the transaction.", re);
            }
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }
        return obj;
    }

    //--------------------------

    public static List<Object> getObjectsBySqlNew(String query) {

        Query q;
        List<Object> obj = null;
        EntityManager entityManager = null;

        try {

            entityManager = JPAUtil.getEntityManagerFactory().createEntityManager();
            entityManager.getTransaction().begin();
            String finalQuery = query;
            q = entityManager.createNativeQuery(finalQuery);

            obj = q.getResultList();
            entityManager.getTransaction().commit();
        } catch (Exception ex) {
            log.error("Error in getObjectsBySqlQuery: " + ex.getLocalizedMessage());
            try {
                entityManager.getTransaction().rollback();
            } catch (Exception re) {
                log.error("An error occurred attempting to roll back the transaction.", re);
            }
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }
        return obj;
    }

    //--------------------------



    public static List getObjectsBySqlQueryObject(String query) {
        Query q;
        List results = null;
        EntityManager entityManager = null;
        try {
            entityManager = JPAUtil.getEntityManagerFactory().createEntityManager();
            entityManager.getTransaction().begin();
            String finalQuery = "SELECT * " + query;

            q = entityManager.createNativeQuery(finalQuery);
            //q.setResultTransformer(Criteria.ALIAS_TO_ENTITY_MAP);
            results = q.getResultList();
            entityManager.getTransaction().commit();
        } catch (Exception ex) {
            log.error("Error in getObjectsByCustomQuery: " + ex.getLocalizedMessage());
            try {
                entityManager.getTransaction().rollback();
            } catch (Exception re) {
                log.error("An error occurred attempting to roll back the transaction.", re);
            }
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }
        return results;
    }//EoM getObjectsBySqlQuery

    public static List getObjectsBySqlQry(String query) {
        Query q;
        List results = null;
        EntityManager entityManager = null;
        try {
            entityManager = JPAUtil.getEntityManagerFactory().createEntityManager();
            entityManager.getTransaction().begin();
            String finalQuery = query;

            q = entityManager.createNativeQuery(finalQuery);
            //q.setResultTransformer(Criteria.ALIAS_TO_ENTITY_MAP);
            results = q.getResultList();
            entityManager.getTransaction().commit();
        } catch (Exception ex) {
            log.error("Error in getObjectsByCustomQuery: " + ex.getLocalizedMessage());
            try {
                entityManager.getTransaction().rollback();
            } catch (Exception re) {
                log.error("An error occurred attempting to roll back the transaction.", re);
            }
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }
        return results;
    }

    public static Object getObjectsBySqlQry1(String query) {
        Query q;
        Object results = null;
        EntityManager entityManager = null;
        try {
            entityManager = JPAUtil.getEntityManagerFactory().createEntityManager();
            entityManager.getTransaction().begin();
            String finalQuery = query;

            q = entityManager.createNativeQuery(finalQuery);
            //q.setResultTransformer(Criteria.ALIAS_TO_ENTITY_MAP);
            results = q.getSingleResult();
            entityManager.getTransaction().commit();
        } catch (Exception ex) {
            log.error("Error in getObjectsByCustomQuery: " + ex.getLocalizedMessage());
            try {
                entityManager.getTransaction().rollback();
            } catch (Exception re) {
                log.error("An error occurred attempting to roll back the transaction.", re);
            }
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }
        return results;
    }


    public static <T> List<T> getAllObjects(Class<T> clazz) {
        return (List<T>) getAllObjects(clazz.getCanonicalName());
    }

    /**
     * Επιστρέφει όλα τα αντικείμενα (εγγραφές) ενός πίνακα.
     *
     * @param classname
     * @return
     */
    public static List<Object> getAllObjects(String classname) {

        //Hibernate Initialisation
        EntityManager entityManager = null;
        //Model Initialisation
        List<Object> retlist = null;
        try {
            entityManager = JPAUtil.getEntityManagerFactory().createEntityManager();
            entityManager.getTransaction().begin();

            String query = "from " + classname + " c ";
            log.debug("dbTransactions.getAllObjects(): Querying: select * " + query);
            Query q = entityManager.createQuery(query);


            //Perform The Query
            retlist = (List<Object>) q.getResultList();

            entityManager.getTransaction().commit();

        } catch (Exception ex) {
            log.error("dbTransactions.getAllObjects(" + classname + "): An error occurred during the transaction. Attempting to RollBack", ex);
            try {
                entityManager.getTransaction().rollback();
            } catch (Exception re) {
                log.error("dbTransactions.getAllObjects(" + classname + "): An error occurred attempting to roll back the transaction.", re);
            }
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }

        log.debug("dbTransactions.getAllObjects(" + classname + ") returns List<Object>.size:" + ((retlist != null) ? retlist.size() : null));
        return retlist;
    }//EoM getAllObjects

    /**
     * Method getAllObjects(String classname) loads all hibernate Objects from
     * the database. This method should be avoided if the amount of entries is
     * excessive
     *
     * @param classname The classname as mapped in the hibernate configuration
     * @return The hibernate List of classes or null
     */
//    public static ScrollableResults getAllObjectsScrolled(String classname) {
//        //log.info("dbTransactions.getAllObjects("+classname+") called");
//
//        //Hibernate Initialisation
//        EntityManager entityManager = null;
//        //Model Initialisation
//        ScrollableResults retlist = null;
//        try {
//             entityManager = JPAUtil.getEntityManagerFactory().createEntityManager();
//            entityManager.getTransaction().begin();
//
//            String query = "from " + classname + " c ";
//            //log.debug("dbTransactions.getAllObjects(): Querying: "+query);
//            Query q = entityManager.createQuery(query);
//
//
//            //Perform The Query
//            retlist = q.scroll();
//
//            entityManager.getTransaction().commit();
//
//        } catch (Exception ex) {
//            log.error("dbTransactions.getAllObjects(" + classname + "): An error occurred during the transaction. Attempting to RollBack", ex);
//            try {
//                entityManager.getTransaction().rollback();
//            } catch (Exception re) {
//                log.error("dbTransactions.getAllObjects(" + classname + "): An error occurred attempting to roll back the transaction.", re);
//            }
//        } finally {
//            if (entityManager != null) {
//                entityManager.close();
//            }
//        }
//
//        log.debug("dbTransactions.getAllObjects(" + classname + ") returns List<Object>.size:" + ((retlist != null) ? retlist : null));
//        return retlist;
//    }//EoM getAllObjects

    /**
     * Method getObjectsPaginated(String classname, int page, int pagesize)
     * loads hibernate Objects from the database that belong to specific
     * page(i.e. not all objects). This method should be used to improve
     * performance during data-fetch
     *
     * @param classname The classname as mapped in the hibernate configuration
     * @param page      The required page pivot
     * @param pagesize  The required page size
     * @return The hibernate List of classes or null
     */
    public static List<Object> getObjectsPaginated(String classname, int page, int pagesize) {
        log.debug("dbTransactions.getObjectsPaginated(" + classname + "," + page + "," + pagesize + ") called");

        //Hibernate Initialisation
        EntityManager entityManager = null;
        //Model Initialisation
        List<Object> retlist = null;
        try {
            entityManager = JPAUtil.getEntityManagerFactory().createEntityManager();
            entityManager.getTransaction().begin();
            String query = "from " + classname + " c ";
            log.info("dbTransactions.getObjectsPaginated(" + classname + "," + page + "," + pagesize + "): Querying: " + query);  //sos to be added
            Query q = entityManager.createQuery(query);

            q.setFirstResult((page - 1) * pagesize);
            q.setMaxResults(pagesize);

            //Perform The Query
            retlist = q.getResultList();

            entityManager.getTransaction().commit();

        } catch (Exception ex) {
            log.error("dbTransactions.getObjectsPaginated(" + classname + "," + page + "," + pagesize + "): An error occurred during the transaction. Attempting to RollBack", ex);
            try {
                entityManager.getTransaction().rollback();
            } catch (Exception re) {
                log.error("dbTransactions.getObjectsPaginated(" + classname + "," + page + "," + pagesize + "): An error occurred attempting to roll back the transaction.", re);
            }
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }

        log.info("dbTransactions.getObjectsPaginated(" + classname + "," + page + "," + pagesize + ") returns List<Object>.size:" + ((retlist != null) ? retlist.size() : null));
        return retlist;
    }//EoM getObjectsPaginated

    public static List<Object> getAllObjectsSortedDistinct2(String classname, String sortproperty,
                                                            int sorttype, String distinctproperty1, String distinctproperty2) {
        log.debug("dbTransactions.getAllObjectsSorted(" + classname + "," + sortproperty + "," + sorttype + ") called");

        //Hibernate Initialisation
        EntityManager entityManager = null;
        //Model Initialisation
        List<Object> retlist = null;
        try {
            entityManager = JPAUtil.getEntityManagerFactory().createEntityManager();
            entityManager.getTransaction().begin();
            String query = "select distinct c." + distinctproperty1 + ",c." + distinctproperty2 + " from " + classname + " c order by c." + sortproperty + " " + ((sorttype == 0) ? "asc" : "desc");
            log.debug("dbTransactions.getAllObjectsSorted(" + classname + "," + sortproperty + "," + sorttype + "): Querying: " + query);
            Query q = entityManager.createQuery(query);


            //Perform The Query
            retlist = q.getResultList();
            entityManager.getTransaction().commit();

        } catch (Exception ex) {
            log.error("dbTransactions.getAllObjectsSorted(" + classname + "," + sortproperty + "," + sorttype + "): An error occurred during the transaction. Attempting to RollBack", ex);
            try {
                entityManager.getTransaction().rollback();
            } catch (Exception re) {
                log.error("dbTransactions.getAllObjectsSorted(" + classname + "," + sortproperty + "," + sorttype + "): An error occurred attempting to roll back the transaction.", re);
            }
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }

        log.info("dbTransactions.getAllObjectsSorted(" + classname + "," + sortproperty + "," + sorttype + ") returns List<Object>.size:" + ((retlist != null) ? retlist.size() : null));
        return retlist;
    }//EoM getAllObjectsSorted


    /**
     * Method getAllObjectsSortedBykeyProperty(String classname, String sortprop,int sort)
     * loads all hibernate Objects from the database sorted by a class parameter
     * (e.g. firstname). This method should be avoided if the amount of entries
     * is excessive
     *
     * @param classname       The classname as mapped in the hibernate configuration
     * @param sortkeyproperty The property that will be used during sorting
     * @param sorttype        If 0 Sorting will be ASC else Sorting will be DESC
     * @return The hibernate List of classes or null
     */
    public static List<Object> getAllObjectsSortedBykeyProperty(String classname, String sortkeyproperty,
                                                                int sorttype) {

        //Hibernate Initialisation
        EntityManager entityManager = null;
        //Model Initialisation
        List<Object> retlist = null;
        try {
            entityManager = JPAUtil.getEntityManagerFactory().createEntityManager();
            entityManager.getTransaction().begin();
            String query = "from " + classname + " c order by c.id." + sortkeyproperty + " " + ((sorttype == 0) ? "asc" : "desc");
            log.debug("dbTransactions.getAllObjectsSortedBykeyProperty(" + classname + "," + sortkeyproperty + "," + sorttype + "): Querying: " + query);
            Query q = entityManager.createQuery(query);


            //Perform The Query
            retlist = q.getResultList();
            entityManager.getTransaction().commit();

        } catch (Exception ex) {
            log.error("dbTransactions.getAllObjectsSortedBykeyProperty(" + classname + "," + sortkeyproperty + "," + sorttype + "): An error occurred during the transaction. Attempting to RollBack", ex);
            try {
                entityManager.getTransaction().rollback();
            } catch (Exception re) {
                log.error("dbTransactions.getAllObjectsSortedBykeyProperty(" + classname + "," + sortkeyproperty + "," + sorttype + "): An error occurred attempting to roll back the transaction.", re);
            }
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }

        log.info("dbTransactions.getAllObjectsSortedBykeyProperty(" + classname + "," + sortkeyproperty + "," + sorttype + ") returns List<Object>.size:" + ((retlist != null) ? retlist.size() : null));
        return retlist;
    }//EoM getAllObjectsSortedBykeyProperty


    /**
     * Method getAllObjectsSorted(String classname, String sortprop,int sort)
     * loads all hibernate Objects from the database sorted by a class parameter
     * (e.g. firstname). This method should be avoided if the amount of entries
     * is excessive
     *
     * @param classname    The classname as mapped in the hibernate configuration
     * @param sortproperty The property that will be used during sorting
     * @param sorttype     If 0 Sorting will be ASC else Sorting will be DESC
     * @return The hibernate List of classes or null
     */
    public static List<Object> getAllObjectsSorted(String classname, String sortproperty, int sorttype) {
        //log.debug("dbTransactions.getAllObjectsSorted("+classname+","+sortproperty+","+sorttype+") called");

        //Hibernate Initialisation
        EntityManager entityManager = null;
        //Model Initialisation
        List<Object> retlist = null;
        try {
            entityManager = JPAUtil.getEntityManagerFactory().createEntityManager();
            entityManager.getTransaction().begin();
            String query = "from " + classname + " c order by c." + sortproperty + " " + ((sorttype == 0) ? "asc" : "desc");
            log.debug("dbTransactions.getAllObjectsSorted(" + classname + "," + sortproperty + "," + sorttype + "): Querying: " + query);
            Query q = entityManager.createQuery(query);

            //Perform The Query
            retlist = q.getResultList();
            entityManager.getTransaction().commit();
//            HibernateUtil.getSessionFactory().getCache().evictEntity(classname, 1l);

        } catch (Exception ex) {
            log.error("dbTransactions.getAllObjectsSorted(" + classname + "," + sortproperty + "," + sorttype + "): An error occurred during the transaction. Attempting to RollBack", ex);
            try {
                entityManager.getTransaction().rollback();
            } catch (Exception re) {
                log.error("dbTransactions.getAllObjectsSorted(" + classname + "," + sortproperty + "," + sorttype + "): An error occurred attempting to roll back the transaction.", re);
            }
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }

        log.info("dbTransactions.getAllObjectsSorted(" + classname + "," + sortproperty + "," + sorttype + ") returns List<Object>.size:" + ((retlist != null) ? retlist.size() : null));
        return retlist;
    }//EoM getAllObjectsSorted

    /**
     * Method countAllObjects(String classname) counts all hibernate Objects
     * from the database without loading them (lazy way)
     *
     * @param classname The classname as mapped in the hibernate configuration
     * @return The amount of classes or null
     */
    public static Integer countAllObjects(String classname) {
        log.debug("dbTransactions.countAllObjects(" + classname + ") called");
        //Hibernate Initialisation
        EntityManager entityManager = null;
        //Initialisation
        Integer count = 0;
        try {
            entityManager = JPAUtil.getEntityManagerFactory().createEntityManager();
            entityManager.getTransaction().begin();

            count = Integer.parseInt (entityManager.createQuery("select count(*) from " + classname).getSingleResult().toString());

            entityManager.getTransaction().commit();

        } catch (Exception ex) {
            log.error("dbTransactions.countAllObjects(" + classname + "): An error occurred during the transaction. Attempting to RollBack", ex);
            try {
                entityManager.getTransaction().rollback();
            } catch (Exception re) {
                log.error("dbTransactions.countAllObjects(" + classname + "): An error occurred attempting to roll back the transaction.", re);
            }
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }

        log.info("dbTransactions.countAllObjects(" + classname + ") returns count:" + count);

        return count;
    }//EoM countAllObjects

    /**
     * Method countAllObjects(String classname) counts all hibernate Objects
     * from the database without loading them (lazy way)
     *
     * @param classname The classname as mapped in the hibernate configuration
     * @return The amount of classes or null
     */
    public static Integer countObjectsByProperty(String classname, String propertyName) {
        log.debug("dbTransactions.countAllObjects(" + classname + ") called");
        //Hibernate Initialisation
        EntityManager entityManager = null;
        //Initialisation
        Integer count = 0;
        try {
            entityManager = JPAUtil.getEntityManagerFactory().createEntityManager();
            entityManager.getTransaction().begin();

            String query = "Select count(*) from " + classname + " c group by c." + propertyName;
            count = (Integer) entityManager.createQuery(query).getSingleResult();
            entityManager.getTransaction().commit();

        } catch (Exception ex) {
            log.error("dbTransactions.countAllObjects(" + classname + "): An error occurred during the transaction. Attempting to RollBack", ex);
            try {
                entityManager.getTransaction().rollback();
            } catch (Exception re) {
                log.error("dbTransactions.countAllObjects(" + classname + "): An error occurred attempting to roll back the transaction.", re);
            }
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }

        log.info("dbTransactions.countAllObjects(" + classname + ") returns count:" + count);

        return count;
    }//EoM countAllObjects

    /**
     * Method getMaxByProperty(String classname)
     *
     * @param classname The classname as mapped in the hibernate configuration
     * @return The amount of classes or null
     */
    public static Object getMaxByProperty(String classname, String propertyName) {

        EntityManager entityManager = null;
        Object obj = null;
        try {
            entityManager = JPAUtil.getEntityManagerFactory().createEntityManager();
            entityManager.getTransaction().begin();

            Query q = entityManager.createQuery("select max(" + propertyName + ") from " + classname);
            obj = q.getSingleResult();

            entityManager.getTransaction().commit();

        } catch (Exception ex) {
            log.error("dbTransactions.countAllObjects(" + classname + "): An error occurred during the transaction. Attempting to RollBack", ex);
            try {
                entityManager.getTransaction().rollback();
            } catch (Exception re) {
                log.error("dbTransactions.countAllObjects(" + classname + "): An error occurred attempting to roll back the transaction.", re);
            }
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }

        log.info("dbTransactions.countAllObjects(" + classname);

        return obj;
    }//EoM getMaxByProperty

    /**
     * Method getObjectsByProperty(String classname, String propertyname,String
     * propertyvalue) loads all hibernate Objects from the database after
     * applying a custom where clause related to the property (e.g.
     * "firstname","first1").
     *
     * @param classname     The classname as mapped in the hibernate configuration
     * @param propertyname  The property that will be used during sorting
     * @param propertyvalue The property value that will be used during sorting
     * @return The hibernate List of classes or null
     */
    public static List<Object> getObjectsByProperty(String classname, String propertyname, Object propertyvalue) {
        //log.debug("dbTransactions.getObjectsByProperty("+classname+","+propertyname+","+propertyvalue+") called");

        //Hibernate Initialisation
        //Model Initialisation
        EntityManager entityManager = null;
        List<Object> retlist = null;
        try {
            entityManager = JPAUtil.getEntityManagerFactory().createEntityManager();
            entityManager.getTransaction().begin();


            String query = "from " + classname + " c where c." + propertyname + "=:param1";
            log.debug("dbTransactions.getObjectsByProperty(" + classname + "," + propertyname + "," + propertyvalue + "): Querying: " + query);
            Query q = entityManager.createQuery(query);
//

            if (propertyvalue instanceof String) {
                q.setParameter("param1", (String) propertyvalue);
            } else if (propertyvalue instanceof Long) {
                q.setParameter("param1", (Long) propertyvalue);
            } else if (propertyvalue instanceof Integer) {
                q.setParameter("param1", (Integer) propertyvalue);
            } else if (propertyvalue instanceof BigDecimal) {
                q.setParameter("param1", (BigDecimal) propertyvalue);
            } else if (propertyvalue instanceof Short) {
                q.setParameter("param1", (Short) propertyvalue);
            } else if (propertyvalue instanceof Date) {
                q.setParameter("param1", (Date) propertyvalue);
            }
            //Perform The Query
            retlist = q.getResultList();
            entityManager.getTransaction().commit();

        } catch (Exception ex) {
            log.error("dbTransactions.getObjectsByProperty(" + classname + "," + propertyname + "," + propertyvalue + "): An error occurred during the transaction. Attempting to RollBack", ex);
            try {
                entityManager.getTransaction().rollback();
            } catch (Exception re) {
                log.error("dbTransactions.getObjectsByProperty(" + classname + "," + propertyname + "," + propertyvalue + "): An error occurred attempting to roll back the transaction.", re);
            }
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }

        log.info("dbTransactions.getObjectsByProperty(" + classname + "," + propertyname + "," + propertyvalue + ") returns List<Object>.size:" + ((retlist != null) ? retlist.size() : null));
        return retlist;
    }//EoM getObjectsByProperty

    public static List<Object> getObjectsByPropertyDateNull(String classname, String propertyname) {
        //log.debug("dbTransactions.getObjectsByProperty("+classname+","+propertyname+","+propertyvalue+") called");

        //Hibernate Initialisation
        //Model Initialisation
        EntityManager entityManager = null;
        List<Object> retlist = null;
        try {
            entityManager = JPAUtil.getEntityManagerFactory().createEntityManager();
            entityManager.getTransaction().begin();


            String query = "from " + classname + " c where c." + propertyname + " IS NULL";
            log.debug("dbTransactions.getObjectsByProperty(" + classname + "," + propertyname + "," + "): Querying: " + query);
            Query q = entityManager.createQuery(query);

            //Perform The Query
            retlist = q.getResultList();
            entityManager.getTransaction().commit();

        } catch (Exception ex) {
            log.error("dbTransactions.getObjectsByProperty(" + classname + "," + propertyname + "," + "): An error occurred during the transaction. Attempting to RollBack", ex);
            try {
                entityManager.getTransaction().rollback();
            } catch (Exception re) {
                log.error("dbTransactions.getObjectsByProperty(" + classname + "," + propertyname + "," + "): An error occurred attempting to roll back the transaction.", re);
            }
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }

        log.info("dbTransactions.getObjectsByProperty(" + classname + "," + propertyname + "," + ") returns List<Object>.size:" + ((retlist != null) ? retlist.size() : null));
        return retlist;
    }//EoM getObjectsByProperty

    /**
     * Method getObjectsByProperty(String classname, String propertyname,String
     * propertyvalue) loads all hibernate Objects from the database after
     * applying a custom where clause related to the property (e.g.
     * "firstname","first1").
     *
     * @param classname     The classname as mapped in the hibernate configuration
     * @param propertyname  The property that will be used during sorting
     * @param propertyvalue The property value that will be used during sorting
     * @return The hibernate List of classes or null
     */
    public static List<Object> getObjectsByPropertyPaginated(String classname, String propertyname, Object
            propertyvalue, Integer page, Integer pageSize) {

        EntityManager entityManager = null;
        List<Object> retlist = null;

        try {
            entityManager = JPAUtil.getEntityManagerFactory().createEntityManager();
            entityManager.getTransaction().begin();
            String query = "from " + classname + " c where c." + propertyname + "=:param1";

            Query q = entityManager.createQuery(query);

            q.setParameter("param1", propertyvalue);


            // Paginate
            q.setFirstResult((page - 1) * pageSize);
            q.setMaxResults(pageSize);

            // Perform The Query
            retlist = q.getResultList();
            entityManager.getTransaction().commit();

        } catch (Exception ex) {
            log.error("dbTransactions.getObjectsByProperty(" + classname + "," + propertyname + "," + propertyvalue + "): An error occurred during the transaction. Attempting to RollBack", ex);
            try {
                entityManager.getTransaction().rollback();
            } catch (Exception re) {
                log.error("dbTransactions.getObjectsByProperty(" + classname + "," + propertyname + "," + propertyvalue + "): An error occurred attempting to roll back the transaction.", re);
            }
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }

        log.info("dbTransactions.getObjectsByProperty(" + classname + "," + propertyname + "," + propertyvalue + ") returns List<Object>.size:" + ((retlist != null) ? retlist.size() : null));
        return retlist;
    }//EoM getObjectsByProperty

    /**
     * Method getObjectsByPropertyLike(String classname, String
     * propertyname,String propertyvalue) loads all hibernate Objects from the
     * database after applying a custom where clause related to the property
     * (e.g. "firstname","first1").
     *
     * @param classname     The classname as mapped in the hibernate configuration
     * @param propertyname  The property that will be used during sorting
     * @param propertyvalue The property value that will be used during sorting
     * @return The hibernate List of classes or null
     */
    public static List<Object> getObjectsByPropertyLike(String classname, String propertyname, String
            propertyvalue, String sortproperty, int sorttype) {

        EntityManager entityManager = null;
        List<Object> retlist = null;

        try {
            entityManager = JPAUtil.getEntityManagerFactory().createEntityManager();
            entityManager.getTransaction().begin();
            String query = "from " + classname + " c where c." + propertyname + " like :param1" + " order by c." + sortproperty + " " + ((sorttype == 0) ? "asc" : "desc");
            log.debug("dbTransactions.getObjectsByProperty(" + classname + "," + propertyname + "," + propertyvalue + "): Querying: " + query);
            Query q = entityManager.createQuery(query);
            if (propertyvalue == null) {
                propertyvalue = "";
            }
            q.setParameter("param1", propertyvalue + "%");

            //Perform The Query
            retlist = q.getResultList();
            entityManager.getTransaction().commit();

        } catch (Exception ex) {
            log.error("dbTransactions.getObjectsByProperty(" + classname + "," + propertyname + "," + propertyvalue + "): An error occurred during the transaction. Attempting to RollBack", ex);
            try {
                entityManager.getTransaction().rollback();
            } catch (Exception re) {
                log.error("dbTransactions.getObjectsByProperty(" + classname + "," + propertyname + "," + propertyvalue + "): An error occurred attempting to roll back the transaction.", re);
            }
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }

        log.info("dbTransactions.getObjectsByProperty(" + classname + "," + propertyname + "," + propertyvalue + ") returns List<Object>.size:" + ((retlist != null) ? retlist.size() : null));
        return retlist;
    }//EoM getObjectsByProperty

    public static void storeObjectsDeleteObjects(List storeObjects, List deleteObjects) {
        log.debug("dbTransactions.storeObjectsUpdateObjects(" + storeObjects + "," + deleteObjects + ") called");

        //Hibernate Initialisation
        EntityManager entityManager = null;
        try {
            entityManager = JPAUtil.getEntityManagerFactory().createEntityManager();
            entityManager.getTransaction().begin();

            for (Object obj : deleteObjects) {
                entityManager.remove(obj);
            }

            for (Object obj : storeObjects) {
                entityManager.persist(obj);
            }

            entityManager.flush();
            entityManager.getTransaction().commit();

        } catch (Exception ex) {
            log.error(ex.getMessage());
            log.error("dbTransactions.storeObjectsUpdateObjects(" + storeObjects + "," + deleteObjects + "): An error occurred during the transaction. Attempting to RollBack", ex);
            try {
                entityManager.getTransaction().rollback();
            } catch (Exception re) {
                log.error("dbTransactions.storeObjectsUpdateObjects(" + storeObjects + "," + deleteObjects + "): An error occurred attempting to roll back the transaction.", re);
            }
            FacesMessage facesMessage = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error in  insert", "Something went Wrong in insert");
            FacesContext.getCurrentInstance().addMessage(null, facesMessage);
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }
    }//EoM storeObjectsDeleteObjects



    public static Object storeObject(Object obj) {
        log.debug("dbTransactions.storeObject(" + obj + ") called");

        //Hibernate Initialisation
        EntityManager entityManager = null;
        try {
            entityManager = JPAUtil.getEntityManagerFactory().createEntityManager();
            EntityTransaction tx = entityManager.getTransaction();
            tx.begin();

            entityManager.persist(obj);
            entityManager.flush();

            tx.commit();

        } catch (Exception ex) {
            log.error(ex.getMessage());
            log.error("dbTransactions.storeObject(" + obj + "): An error occurred during the transaction. Attempting to RollBack", ex);
            try {
                entityManager.getTransaction().rollback();
            } catch (Exception re) {
                log.error("dbTransactions.storeObject(" + obj + "): An error occurred attempting to roll back the transaction.", re);
            }
            FacesMessage facesMessage = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error in insert", "Something went Wrong in insert");
            FacesContext.getCurrentInstance().addMessage(null, facesMessage);
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }
        return (obj);
    }//EoM storeObject


    public static Object storeWithMergeObject(Object obj) {
        log.debug("dbTransactions.storeObject(" + obj + ") called");

        //Hibernate Initialisation
        EntityManager entityManager = null;
        try {
            entityManager = JPAUtil.getEntityManagerFactory().createEntityManager();
            EntityTransaction tx = entityManager.getTransaction();
            tx.begin();

            entityManager.merge(obj);

//            entityManager.flush();
            tx.commit();

        } catch (Exception ex) {
            log.error(ex.getMessage());
            log.error("dbTransactions.storeObject(" + obj + "): An error occurred during the transaction. Attempting to RollBack", ex);
            try {
                entityManager.getTransaction().rollback();
            } catch (Exception re) {
                log.error("dbTransactions.storeObject(" + obj + "): An error occurred attempting to roll back the transaction.", re);
            }
            FacesMessage facesMessage = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error in insert", "Something went wrong in insert");
            FacesContext.getCurrentInstance().addMessage(null, facesMessage);
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }
        return (obj);
    }//EoM storeObject

    /**
     * @param classname
     * @param query
     * @param params
     * @return
     */
//    public static Object storeObjectGetId(Object obj) {
//        log.debug("dbTransactions.storeObject(" + obj + ") called");
//
//        //Hibernate Initialisation
//        EntityManager entityManager = null;
//        try {
//             entityManager = JPAUtil.getEntityManagerFactory().createEntityManager();
//            entityManager.getTransaction().begin();
//
//            entityManager.persist(obj);
//            entityManager.flush();
//
//            Long ret = (Long) obj.getId();
//
//            log.info("-->" + ret);
//            entityManager.getTransaction().commit();
//
//        } catch (Exception ex) {
//            log.error(ex.getMessage());
//            log.error("dbTransactions.storeObject(" + obj + "): An error occurred during the transaction. Attempting to RollBack", ex);
//            try {
//                entityManager.getTransaction().rollback();
//            } catch (Exception re) {
//                log.error("dbTransactions.storeObject(" + obj + "): An error occurred attempting to roll back the transaction.", re);
//            }
//        } finally {
//            if (entityManager != null) {
//                entityManager.close();
//            }
//            return obj;
//        }
//    }//EoM storeObject

    /**
     * @param classname
     * @param query
     * @param params
     * @return
     */
//    public static int storeObjectId(Object obj) {
//        log.debug("dbTransactions.storeObject(" + obj + ") called");
//        int ret = -1;
//        //Hibernate Initialisation
//        EntityManager entityManager = null;
//        try {
//             entityManager = JPAUtil.getEntityManagerFactory().createEntityManager();
//            entityManager.getTransaction().begin();
//
//            Integer integer = (Integer) entityManager.persist(obj);
//            ret = integer;
//            entityManager.getTransaction().commit();
//
//        } catch (Exception ex) {
//            log.error(ex.getMessage());
//            log.error("dbTransactions.storeObject(" + obj + "): An error occurred during the transaction. Attempting to RollBack", ex);
//            try {
//                entityManager.getTransaction().rollback();
//            } catch (Exception re) {
//                log.error("dbTransactions.storeObject(" + obj + "): An error occurred attempting to roll back the transaction.", re);
//            }
//        } finally {
//            if (entityManager != null) {
//                entityManager.close();
//            }
//        }
//        return ret;
//}//EoM storeObject

    /**
     * @param obj
     * @return
     */
    public static void updateObject(Object obj) {
        log.debug("dbTransactions.updateObject(" + obj + ") called");

        //Hibernate Initialisation
        EntityManager entityManager = null;
        try {
            entityManager = JPAUtil.getEntityManagerFactory().createEntityManager();
            entityManager.getTransaction().begin();

//            entityManager.merge(obj);
//            entityManager.flush();

            // https://vladmihalcea.com/jpa-persist-merge-hibernate-save-update-saveorupdate/
            Session session = entityManager.unwrap(Session.class);

            session.update(obj);
            entityManager.flush();

            entityManager.getTransaction().commit();

        } catch (Exception ex) {
            try {
                entityManager.getTransaction().rollback();
            } catch (Exception re) {
                log.info("dbTransactions.updateObject(" + obj + "): An error occurred attempting to roll back the transaction." + re);
            }

            FacesMessage facesMessage = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error in update", "Something went wrong in update");
            FacesContext.getCurrentInstance().addMessage(null, facesMessage);
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }
        log.info("dbTransactions.updateObject(" + obj + ") returns");
    }//EoM updateObject


    /**
     * @param obj
     * @return
     */
    public static void saveOrUpdateObject(Object obj) {
        log.debug("dbTransactions.saveOrUpdateObject(" + obj + ") called");

        //Hibernate Initialisation
        EntityManager entityManager = null;
        try {
            entityManager = JPAUtil.getEntityManagerFactory().createEntityManager();
            entityManager.getTransaction().begin();

            // https://vladmihalcea.com/jpa-persist-merge-hibernate-save-update-saveorupdate/
            Session session = entityManager.unwrap(Session.class);

            session.saveOrUpdate(obj);
            entityManager.flush();

            entityManager.getTransaction().commit();

        } catch (Exception ex) {
            try {
                entityManager.getTransaction().rollback();
            } catch (Exception re) {
                log.info("dbTransactions.saveOrUpdateObject(" + obj + "): An error occurred attempting to roll back the transaction." + re);
            }

            FacesMessage facesMessage = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error in save or update", "Something went wrong in save or update");
            FacesContext.getCurrentInstance().addMessage(null, facesMessage);
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }
        log.info("dbTransactions.saveOrUpdateObject(" + obj + ") returns");
    }//EoM saveOrUpdateObject



    /**
     * @param classname The classname as mapped in the hibernate configuration
     * @return
     */
    public Set convertListToSet(List list, String classname) {
        Set ret = new HashSet(0);
        for (Iterator iter = list.iterator(); iter.hasNext(); ) {
            Object obj1 = iter.next();
            ret.add(obj1);
            //rest of the code block removed
        }
        return ret;
    }//EoM convertListToSet

    /**
     * Method getObjectsByManyToOneLong(String classname,String
     * manyToOnename,String refidname,String manyToOnevalue) loads all hibernate
     * Objects from the database after applying a custom where clause related to
     * the property (e.g. "firstname","first1").
     *
     * @param classname     The classname as mapped in the hibernate configuration
     * @param manyToOnename  The property that will be used during sorting
     * @param refidname The property value that will be used during sorting
     * @return The hibernate List of classes or null
     */
    public static List<Object> getObjectsByManyToOneLong(String classname, String manyToOnename, String
            refidname, String manyToOnevalue) {
        log.debug("dbTransactions.getObjectsByProperty(" + classname + ", called");

        //Hibernate Initialisation
        EntityManager entityManager = null;
        //Model Initialisation
        List<Object> retlist = null;
        try {
            entityManager = JPAUtil.getEntityManagerFactory().createEntityManager();
            entityManager.getTransaction().begin();
            String query = "from " + classname + " c where c." + manyToOnename + "." + refidname + "=:param1";
            log.debug("dbTransactions.getObjectsByProperty(" + classname + "): Querying: " + query);
            Query q = entityManager.createQuery(query);
            q.setParameter("param1", new Long(manyToOnevalue));

            //Perform The Query
            retlist = q.getResultList();

            entityManager.getTransaction().commit();

        } catch (Exception ex) {
            log.error(ex.getMessage());
            log.error("dbTransactions.getObjectsByProperty(" + classname + "),: An error occurred during the transaction. Attempting to RollBack", ex);
            try {
                entityManager.getTransaction().rollback();
            } catch (Exception re) {
                log.error("dbTransactions.getObjectsByProperty(" + classname + "): An error occurred attempting to roll back the transaction.", re);
            }
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }

        log.info("dbTransactions.getObjectsByProperty(" + classname + ") returns List<Object>.size:" + ((retlist != null) ? retlist.size() : null));
        return retlist;
    }//EoM getObjectsByProperty

    /**
     * Method getObjectsByManyToOneString(String classname,String
     * manyToOnename,String refidname,String manyToOnevalue) loads all hibernate
     * Objects from the database after applying a custom where clause related to
     * the property (e.g. "firstname","first1").
     *
     * @param classname     The classname as mapped in the hibernate configuration
     * @param manyToOnename  The property that will be used during sorting
     * @param refidname The property value that will be used during sorting
     * @return The hibernate List of classes or null
     */
    public static List<Object> getObjectsByManyToOneString(String classname, String manyToOnename, String
            refidname, String manyToOnevalue) {
        //log.debug("dbTransactions.getObjectsByProperty("+classname+","+propertyname+","+propertyvalue+") called");

        //Hibernate Initialisation
        EntityManager entityManager = null;
        //Model Initialisation
        List<Object> retlist = null;
        try {
            entityManager = JPAUtil.getEntityManagerFactory().createEntityManager();
            entityManager.getTransaction().begin();
            String query = "from " + classname + " c where c." + manyToOnename + "." + refidname + "=:param1";
            log.debug("dbTransactions.getObjectsByProperty(" + classname + "): Querying: " + query);
            Query q = entityManager.createQuery(query);
            q.setParameter("param1", manyToOnevalue);

            //Perform The Query
            retlist = q.getResultList();

            entityManager.getTransaction().commit();

        } catch (Exception ex) {
            log.error(ex.getMessage());
            log.error("dbTransactions.getObjectsByProperty(" + classname + "): An error occurred during the transaction. Attempting to RollBack", ex);
            try {
                entityManager.getTransaction().rollback();
            } catch (Exception re) {
                log.error("dbTransactions.getObjectsByProperty(" + classname + "): An error occurred attempting to roll back the transaction.", re);
            }
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }

        log.info("dbTransactions.getObjectsByProperty(" + classname + ") returns List<Object>.size:" + ((retlist != null) ? retlist.size() : null));
        return retlist;
    }//EoM getObjectsByProperty

    /**
     * Method Object deleteObject(Object obj) deletes from the database the
     * according record of the object obj, based on primary key(unique or
     * composite ID)
     *
     * @param obj The object which accords to the record that we want to delete
     *            from the database
     * @return The hibernate List of classes or null The hibernate List of
     * classes or null
     */
    public static Object deleteObject(Object obj) {

        //Hibernate Initialisation
        EntityManager entityManager = null;
        try {
            entityManager = JPAUtil.getEntityManagerFactory().createEntityManager();
            entityManager.getTransaction().begin();

            entityManager.remove(entityManager.contains(obj) ? obj : entityManager.merge(obj));

            entityManager.flush();
            entityManager.getTransaction().commit();

        } catch (Exception ex) {
            log.info(ex.getMessage());
            try {
                entityManager.getTransaction().rollback();
            } catch (Exception re) {
                re.printStackTrace();
            }

            //   throw new FacesException("Σφάλμα κατά την διαγραφή");
            FacesMessage facesMessage = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error in delete", "Something went wrong in delete");
            FacesContext.getCurrentInstance().addMessage(null, facesMessage);
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }
        return (obj);
    }//EoM getObjectsByProperty

    /**
     * @param classname
     * @param manyToOnename
     * @param refidname
     * @param manyToOnename
     * @return
     */
    public static void deleteObjectsByManyToOneLong(String classname, String manyToOnename, String
            refidname, String manyToOnevalue) {
        log.debug("dbTransactions.getObjectsByProperty(" + classname + ") called");

        //Hibernate Initialisation
        EntityManager entityManager = null;
        //Model Initialisation
        List<Object> retlist = null;
        try {
            entityManager = JPAUtil.getEntityManagerFactory().createEntityManager();
            entityManager.getTransaction().begin();
            String query = "from " + classname + " c where c." + manyToOnename + "." + refidname + "=:param1";
            log.debug("dbTransactions.getObjectsByProperty(" + classname + "): Querying: " + query);
            Query q = entityManager.createQuery(query);
            q.setParameter("param1", new Long(manyToOnevalue));

            //Perform The Query
            retlist = q.getResultList();
            if (retlist != null) {
                Iterator itr = retlist.iterator();
                while (itr.hasNext()) {
                    entityManager.remove(itr.next());
                }//eowhile
            }

            entityManager.getTransaction().commit();

        } catch (Exception ex) {
            log.error(ex.getMessage());
            log.error("dbTransactions.getObjectsByProperty(" + classname + "): An error occurred during the transaction. Attempting to RollBack", ex);
            try {
                entityManager.getTransaction().rollback();
            } catch (Exception re) {
                log.error("dbTransactions.getObjectsByProperty(" + classname + "): An error occurred attempting to roll back the transaction.", re);
            }
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }

        log.info("dbTransactions.getObjectsByProperty(" + classname + "+) returns List<Object>.size:" + ((retlist != null) ? retlist.size() : null));

    }//EoM getObjectsByProperty




    public static void truncateKenObjects() {
        //Hibernate Initialisation
        EntityManager entityManager = null;
        try {
            entityManager = JPAUtil.getEntityManagerFactory().createEntityManager();
            entityManager.getTransaction().begin();
            entityManager.createNativeQuery("truncate table ken_atomika").executeUpdate();
            entityManager.createNativeQuery("truncate table ken_diplwmata_politika").executeUpdate();
            entityManager.createNativeQuery("truncate table ken_epaggelmata").executeUpdate();
            entityManager.createNativeQuery("truncate table ken_epithymia").executeUpdate();
            entityManager.createNativeQuery("truncate table ken_epithymies_eidikothtas").executeUpdate();
            entityManager.createNativeQuery("truncate table ken_epithymies_metatheshs").executeUpdate();
            entityManager.createNativeQuery("truncate table ken_epithymies_topothethshs").executeUpdate();
            entityManager.createNativeQuery("truncate table ken_extra_stoixeia").executeUpdate();
            entityManager.createNativeQuery("truncate table ken_iatrika").executeUpdate();
            entityManager.createNativeQuery("truncate table ken_ikanothtes").executeUpdate();
            entityManager.createNativeQuery("truncate table ken_morfwsh").executeUpdate();
            entityManager.createNativeQuery("truncate table ken_moria_efedrou").executeUpdate();
            entityManager.createNativeQuery("truncate table ken_oikogeneiaka").executeUpdate();
            entityManager.createNativeQuery("truncate table ken_protash").executeUpdate();
            entityManager.createNativeQuery("truncate table ken_swmatometrika").executeUpdate();
            entityManager.createNativeQuery("truncate table ken_sygeneis").executeUpdate();
            entityManager.createNativeQuery("truncate table ken_xenes_glwsses").executeUpdate();
            entityManager.createNativeQuery("truncate table ken_yphresiaka").executeUpdate();
            entityManager.createNativeQuery("truncate table ken_sync_error").executeUpdate();
            entityManager.createNativeQuery("truncate table yea").executeUpdate();
            entityManager.createNativeQuery("delete from ken_stratevmenos").executeUpdate();

            entityManager.getTransaction().commit();

        } catch (Exception ex) {
            log.error(ex.getMessage());
            log.error("dbTransactions.getObjectsByProperty(): An error occurred during the transaction. Attempting to RollBack", ex);
            try {
                entityManager.getTransaction().rollback();
            } catch (Exception re) {
                log.error("dbTransactions.getObjectsByProperty(): An error occurred attempting to roll back the transaction.", re);
            }
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }

        log.info("dbTransactions.getObjectsByProperty(+) returns List<Object>.size:");

    }//EoM getObjectsByProperty

    /**
     * Method getObjectsByManyToOneLong(String classname,String
     * manyToOnename,String refidname,String manyToOnevalue) loads all hibernate
     * Objects from the database after applying a custom where clause related to
     * the property (e.g. "firstname","first1").
     *
     * @param classname     The classname as mapped in the hibernate configuration
     * @return The hibernate List of classes or null
     */
    public static List<Object> getObjectsByManyToOneLongSorted(String classname, String manyToOnename, String
            refidname, String manyToOnevalue, String sortproperty, int sorttype) {
        //log.debug("dbTransactions.getObjectsByProperty("+classname+","+propertyname+","+propertyvalue+") called");

        //Hibernate Initialisation
        EntityManager entityManager = null;
        //Model Initialisation
        List<Object> retlist = null;
        try {
            entityManager = JPAUtil.getEntityManagerFactory().createEntityManager();
            entityManager.getTransaction().begin();
            String query = "from " + classname + " c where c." + manyToOnename + "." + refidname + "=:param1" + " order by c." + sortproperty + " " + ((sorttype == 0) ? "asc" : "desc");

            log.debug("dbTransactions.getObjectsByProperty(" + classname + "+): Querying: " + query);
            Query q = entityManager.createQuery(query);
            q.setParameter("param1", new Long(manyToOnevalue));

            //Perform The Query
            retlist = q.getResultList();

            entityManager.getTransaction().commit();

        } catch (Exception ex) {
            log.error(ex.getMessage());
            log.error("dbTransactions.getObjectsByProperty(" + classname + "): An error occurred during the transaction. Attempting to RollBack", ex);
            try {
                entityManager.getTransaction().rollback();
            } catch (Exception re) {
                log.error("dbTransactions.getObjectsByProperty(" + classname + "): An error occurred attempting to roll back the transaction.", re);
            }
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }

        log.info("dbTransactions.getObjectsByProperty(" + classname + "," + ") returns List<Object>.size:" + ((retlist != null) ? retlist.size() : null));
        return retlist;
    }//EoM getObjectsByProperty

    /**
     * Method getObjectsByKeyProperty(String classname, String
     * keyPropertyname,String keyPropertyvalue) loads all hibernate Objects from
     * the database after applying a custom where clause related to the
     * keyProperty (e.g. "firstname","first1").
     *
     * @param classname        The classname as mapped in the hibernate configuration
     * @param keyPropertyname  The property that will be used during sorting
     * @param keyPropertyvalue The property value that will be used during
     *                         sorting
     * @return The hibernate List of classes or null
     */
    public static List<Object> getObjectsByKeyProperty(String classname, String keyPropertyname, Object
            keyPropertyvalue) {

        //Hibernate Initialisation
        EntityManager entityManager = null;
        //Model Initialisation
        List<Object> retlist = null;
        try {
            entityManager = JPAUtil.getEntityManagerFactory().createEntityManager();
            entityManager.getTransaction().begin();
            String query = "from " + classname + " c where c.id." + keyPropertyname + "=:param1";

            Query q = entityManager.createQuery(query);

            if (keyPropertyvalue instanceof String) {
                q.setParameter("param1", (String) keyPropertyvalue);
            } else if (keyPropertyvalue instanceof Long) {
                q.setParameter("param1", (Long) keyPropertyvalue);
            } else if (keyPropertyvalue instanceof Integer) {
                q.setParameter("param1", (Integer) keyPropertyvalue);
            } else if (keyPropertyvalue instanceof Date) {
                q.setParameter("param1", (Date) keyPropertyvalue);
            } else if (keyPropertyvalue instanceof BigDecimal) {
                q.setParameter("param1", (BigDecimal) keyPropertyvalue);
            }

            //Perform The Query
            retlist = q.getResultList();

            entityManager.getTransaction().commit();

        } catch (Exception ex) {
            log.error("dbTransactions.getObjectsByProperty(" + classname + "," + keyPropertyname + "," + keyPropertyvalue + "): An error occurred during the transaction. Attempting to RollBack", ex);
            try {
                entityManager.getTransaction().rollback();
            } catch (Exception re) {
                log.error("dbTransactions.getObjectsByProperty(" + classname + "," + keyPropertyname + "," + keyPropertyvalue + "): An error occurred attempting to roll back the transaction.", re);
            }
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }

        log.info("dbTransactions.getObjectsByProperty(" + classname + "," + keyPropertyname + "," + keyPropertyvalue + ") returns List<Object>.size:" + ((retlist != null) ? retlist.size() : null));
        return retlist;
    }//EoM getObjectsByProperty

    /**
     * Method getObjectsByKeyPropertySorted(String classname, String
     * keyPropertyname,String keyPropertyvalue,String sortproperty, int
     * sorttype) loads all hibernate Objects from the database after applying a
     * custom where clause related to the keyProperty (e.g.
     * "firstname","first1").
     *
     * @param classname        The classname as mapped in the hibernate configuration
     * @param keyPropertyname  The keyproperty that will be used during search
     * @param keyPropertyvalue The keyproperty value that will be used during
     *                         search
     * @param sortproperty     The property that will be used during sorting
     * @param sorttype         If 0 Sorting will be ASC else Sorting will be DESC
     *                         sorting
     * @return The hibernate List of classes or null
     */
    public static List<Object> getObjectsByKeyPropertySorted(String classname, String keyPropertyname, Object
            keyPropertyvalue, String sortproperty, int sorttype) {

        //Hibernate Initialisation
        EntityManager entityManager = null;
        //Model Initialisation
        List<Object> retlist = null;
        try {
            entityManager = JPAUtil.getEntityManagerFactory().createEntityManager();
            entityManager.getTransaction().begin();
            String query = "from " + classname + " c where c.id." + keyPropertyname + "=:param1" + " order by c.id." + sortproperty + " " + ((sorttype == 0) ? "asc" : "desc");

            Query q = entityManager.createQuery(query);

            if (keyPropertyvalue instanceof String) {
                q.setParameter("param1", (String) keyPropertyvalue);
            } else if (keyPropertyvalue instanceof Long) {
                q.setParameter("param1", (Long) keyPropertyvalue);
            } else if (keyPropertyvalue instanceof Integer) {
                q.setParameter("param1", (Integer) keyPropertyvalue);
            } else if (keyPropertyvalue instanceof Date) {
                q.setParameter("param1", (Date) keyPropertyvalue);
            } else if (keyPropertyvalue instanceof BigDecimal) {
                q.setParameter("param1", (BigDecimal) keyPropertyvalue);
            }

            //Perform The Query
            retlist = q.getResultList();

            entityManager.getTransaction().commit();

        } catch (Exception ex) {
            log.error("dbTransactions.getObjectsByProperty(" + classname + "," + keyPropertyname + "," + keyPropertyvalue + "): An error occurred during the transaction. Attempting to RollBack", ex);
            try {
                entityManager.getTransaction().rollback();
            } catch (Exception re) {
                log.error("dbTransactions.getObjectsByProperty(" + classname + "," + keyPropertyname + "," + keyPropertyvalue + "): An error occurred attempting to roll back the transaction.", re);
            }
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }

        log.info("dbTransactions.getObjectsByKeyPropertySorted(" + classname + "," + keyPropertyname + "," + keyPropertyvalue + ") returns List<Object>.size:" + ((retlist != null) ? retlist.size() : null));
        return retlist;
    }//EoM getObjectsByKeyPropertySorted

    /**
     * Method getObjectsByKeyPropertySorted(String classname, String
     * keyPropertyname,String keyPropertyvalue,String sortproperty, int
     * sorttype) loads all hibernate Objects from the database after applying a
     * custom where clause related to the keyProperty (e.g.
     * "firstname","first1").
     *
     * @param classname        The classname as mapped in the hibernate configuration
     * @param keyPropertyname  The keyproperty that will be used during search
     * @param keyPropertyvalue The keyproperty value that will be used during
     *                         search
     * @param sortproperty     The property that will be used during sorting
     * @param sorttype         If 0 Sorting will be ASC else Sorting will be DESC
     *                         sorting
     * @return The hibernate List of classes or null
     */
    public static List<Object> getObjectsByKeyPropertySorted1(String classname, String keyPropertyname, Object
            keyPropertyvalue, String sortproperty, int sorttype) {

        //Hibernate Initialisation
        EntityManager entityManager = null;
        //Model Initialisation
        List<Object> retlist = null;
        try {
            entityManager = JPAUtil.getEntityManagerFactory().createEntityManager();
            entityManager.getTransaction().begin();
            String query = "from " + classname + " c where c.id." + keyPropertyname + "=:param1" + " order by c." + sortproperty + " " + ((sorttype == 0) ? "asc" : "desc");

            Query q = entityManager.createQuery(query);

            if (keyPropertyvalue instanceof String) {
                q.setParameter("param1", (String) keyPropertyvalue);
            } else if (keyPropertyvalue instanceof Long) {
                q.setParameter("param1", (Long) keyPropertyvalue);
            } else if (keyPropertyvalue instanceof Integer) {
                q.setParameter("param1", (Integer) keyPropertyvalue);
            } else if (keyPropertyvalue instanceof Date) {
                q.setParameter("param1", (Date) keyPropertyvalue);
            } else if (keyPropertyvalue instanceof BigDecimal) {
                q.setParameter("param1", (BigDecimal) keyPropertyvalue);
            }

            //Perform The Query
            retlist = q.getResultList();

            entityManager.getTransaction().commit();

        } catch (Exception ex) {
            log.error("dbTransactions.getObjectsByProperty(" + classname + "," + keyPropertyname + "," + keyPropertyvalue + "): An error occurred during the transaction. Attempting to RollBack", ex);
            try {
                entityManager.getTransaction().rollback();
            } catch (Exception re) {
                log.error("dbTransactions.getObjectsByProperty(" + classname + "," + keyPropertyname + "," + keyPropertyvalue + "): An error occurred attempting to roll back the transaction.", re);
            }
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }

        log.info("dbTransactions.getObjectsByKeyPropertySorted(" + classname + "," + keyPropertyname + "," + keyPropertyvalue + ") returns List<Object>.size:" + ((retlist != null) ? retlist.size() : null));
        return retlist;
    }//EoM getObjectsByKeyPropertySorted


    /**
     * Method getObjectsByTwoKeyPropertySorted(String classname, String
     * keyPropertyname1,String keyPropertyvalue1,String keyPropertyname2,String
     * keyPropertyvalue2,String sortproperty, int sorttype) loads all hibernate
     * Objects from the database after applying a custom where clause related to
     * the keyProperty (e.g. "firstname","first1").
     *
     * @param classname         The classname as mapped in the hibernate configuration
     * @param keyPropertyname1  The keyproperty that will be used during search
     * @param keyPropertyvalue1 The keyproperty value that will be used during
     *                          search
     * @param keyPropertyname2  The keyproperty that will be used during search
     * @param keyPropertyvalue2 The keyproperty value that will be used during
     *                          search
     * @param sortproperty      The property that will be used during sorting
     * @param sorttype          If 0 Sorting will be ASC else Sorting will be DESC
     *                          sorting
     * @return The hibernate List of classes or null
     */
    public static List<Object> getObjectsByTwoKeyPropertySorted(String classname, String keyPropertyname1, Object
            keyPropertyvalue1, String keyPropertyname2, Object keyPropertyvalue2, String sortproperty, int sorttype) {

        //Hibernate Initialisation
        EntityManager entityManager = null;
        //Model Initialisation
        List<Object> retlist = null;
        try {
            entityManager = JPAUtil.getEntityManagerFactory().createEntityManager();
            entityManager.getTransaction().begin();
            String query = "from " + classname + " c where c.id." + keyPropertyname1 + "=:param1 and c.id." + keyPropertyname2 + "=:param2 order by c.id." + sortproperty + " " + ((sorttype == 0) ? "asc" : "desc");

            Query q = entityManager.createQuery(query);


            if (keyPropertyvalue1 instanceof String) {
                q.setParameter("param1", (String) keyPropertyvalue1);
            } else if (keyPropertyvalue1 instanceof Long) {
                q.setParameter("param1", (Long) keyPropertyvalue1);
            } else if (keyPropertyvalue1 instanceof Integer) {
                q.setParameter("param1", (Integer) keyPropertyvalue1);
            } else if (keyPropertyvalue1 instanceof Date) {
                q.setParameter("param1", (Date) keyPropertyvalue1);
            } else if (keyPropertyvalue1 instanceof BigDecimal) {
                q.setParameter("param1", (BigDecimal) keyPropertyvalue1);
            }

            if (keyPropertyvalue2 instanceof String) {
                q.setParameter("param2", (String) keyPropertyvalue2);
            } else if (keyPropertyvalue2 instanceof Long) {
                q.setParameter("param2", (Long) keyPropertyvalue2);
            } else if (keyPropertyvalue2 instanceof Integer) {
                q.setParameter("param2", (Integer) keyPropertyvalue2);
            } else if (keyPropertyvalue2 instanceof Date) {
                q.setParameter("param2", (Date) keyPropertyvalue2);
            } else if (keyPropertyvalue2 instanceof BigDecimal) {
                q.setParameter("param2", (BigDecimal) keyPropertyvalue2);
            }

            //Perform The Query
            retlist = q.getResultList();

            entityManager.getTransaction().commit();

        } catch (Exception ex) {
            log.error("dbTransactions.getObjectsByProperty(" + classname + "," + keyPropertyname1 + "," + keyPropertyvalue1 + "): An error occurred during the transaction. Attempting to RollBack", ex);
            try {
                entityManager.getTransaction().rollback();
            } catch (Exception re) {
                log.error("dbTransactions.getObjectsByProperty(" + classname + "," + keyPropertyname1 + "," + keyPropertyvalue1 + "): An error occurred attempting to roll back the transaction.", re);
            }
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }

        log.info("dbTransactions.getObjectsByProperty(" + classname + ") returns List<Object>.size:" + ((retlist != null) ? retlist.size() : null));
        return retlist;
    }//EoM getObjectsByTwoKeyPropertySorted

    /**
     * @param classname        The classname as mapped in the hibernate configuration
     * @param keyPropertyname  The keyproperty that will be used during search
     * @param keyPropertyvalue The keyproperty value that will be used during
     *                         search
     * @param propertyname     The property that will be used during search
     * @param propertyvalue    The property value that will be used during search
     * @param sortproperty     The key property that will be used during sorting
     * @param sorttype         If 0 Sorting will be ASC else Sorting will be DESC
     *                         sorting
     * @return The hibernate List of classes or null
     */
    public static List<Object> getObjectsByKeyPropertyAndPropertySortedByKeyProperty(String classname, String
            keyPropertyname, Object keyPropertyvalue, String propertyname, Object propertyvalue, String sortproperty,
                                                                                     int sorttype) {

        //Hibernate Initialisation
        EntityManager entityManager = null;
        //Model Initialisation
        List<Object> retlist = null;
        try {
            entityManager = JPAUtil.getEntityManagerFactory().createEntityManager();
            entityManager.getTransaction().begin();
            String query = "from " + classname + " c where c.id." + keyPropertyname + "=:param1 and c." + propertyname + "=:param2 order by c.id." + sortproperty + " " + ((sorttype == 0) ? "asc" : "desc");

            Query q = entityManager.createQuery(query);


            if (keyPropertyvalue instanceof String) {
                q.setParameter("param1", (String) keyPropertyvalue);
            } else if (keyPropertyvalue instanceof Long) {
                q.setParameter("param1", (Long) keyPropertyvalue);
            } else if (keyPropertyvalue instanceof Integer) {
                q.setParameter("param1", (Integer) keyPropertyvalue);
            } else if (keyPropertyvalue instanceof Date) {
                q.setParameter("param1", (Date) keyPropertyvalue);
            } else if (keyPropertyvalue instanceof BigDecimal) {
                q.setParameter("param1", (BigDecimal) keyPropertyvalue);
            }

            if (propertyvalue instanceof String) {
                q.setParameter("param2", (String) propertyvalue);
            } else if (propertyvalue instanceof Long) {
                q.setParameter("param2", (Long) propertyvalue);
            } else if (propertyvalue instanceof Integer) {
                q.setParameter("param2", (Integer) propertyvalue);
            } else if (propertyvalue instanceof Date) {
                q.setParameter("param2", (Date) propertyvalue);
            } else if (propertyvalue instanceof BigDecimal) {
                q.setParameter("param2", (BigDecimal) propertyvalue);
            }

            //Perform The Query
            retlist = q.getResultList();

            entityManager.getTransaction().commit();

        } catch (Exception ex) {
            log.error("dbTransactions.getObjectsByProperty(" + classname + "," + keyPropertyname + "," + keyPropertyvalue + "): An error occurred during the transaction. Attempting to RollBack", ex);
            try {
                entityManager.getTransaction().rollback();
            } catch (Exception re) {
                log.error("dbTransactions.getObjectsByProperty(" + classname + "," + keyPropertyname + "," + keyPropertyvalue + "): An error occurred attempting to roll back the transaction.", re);
            }
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }

        log.info("dbTransactions.getObjectsByProperty(" + classname + "," + keyPropertyname + "," + keyPropertyvalue + ") returns List<Object>.size:" + ((retlist != null) ? retlist.size() : null));
        return retlist;
    }//EoM getObjectsByKeyPropertyAndPropertySortedByKeyProperty

    /**
     * Method getObjectsByPropertySorted(String classname, String
     * propertyname,String propertyvalue,String sortproperty, int sorttype)
     * loads all hibernate Objects from the database after applying a custom
     * where clause related to the property (e.g. "firstname","first1").
     *
     * @param classname     The classname as mapped in the hibernate configuration
     * @param propertyname  The property that will be used during sorting
     * @param propertyvalue The property value that will be used during sorting
     * @param sortproperty  The property that will be used during sorting
     * @param sorttype      If 0 Sorting will be ASC else Sorting will be DESC
     * @return The hibernate List of classes or null
     */
    public static List<Object> getObjectsByPropertySorted(String classname, String propertyname, Object
            propertyvalue, String sortproperty, int sorttype) {
        log.debug("dbTransactions.getObjectsByProperty(" + classname + "," + propertyname + "," + propertyvalue + ") called");

        //Hibernate Initialisation
        EntityManager entityManager = null;
        //Model Initialisation
        List<Object> retlist = null;
        try {
            entityManager = JPAUtil.getEntityManagerFactory().createEntityManager();
            entityManager.getTransaction().begin();

            String query = "from " + classname + " c where c." + propertyname + "=:param1" + " order by c." + sortproperty + " " + ((sorttype == 0) ? "asc" : "desc");
            log.debug("dbTransactions.getObjectsByPropertySorted(" + classname + "," + propertyname + "," + propertyvalue + "): Querying: " + query);
            Query q = entityManager.createQuery(query);


            if (propertyvalue instanceof String) {
                q.setParameter("param1", (String) propertyvalue);
            } else if (propertyvalue instanceof Long) {
                q.setParameter("param1", (Long) propertyvalue);
            } else if (propertyvalue instanceof Integer) {
                q.setParameter("param1", (Integer) propertyvalue);
            } else if (propertyvalue instanceof BigDecimal) {
                q.setParameter("param1", (BigDecimal) propertyvalue);
            } else if (propertyvalue instanceof Short) {
                q.setParameter("param1", (Short) propertyvalue);
            } else if (propertyvalue instanceof Boolean) {
                q.setParameter("param1", (Boolean) propertyvalue);
            }
            //Perform The Query
            retlist = q.getResultList();

            entityManager.getTransaction().commit();

        } catch (Exception ex) {
            log.error("dbTransactions.getObjectsByPropertySorted(" + classname + "," + propertyname + "," + propertyvalue + "): An error occurred during the transaction. Attempting to RollBack", ex);
            try {
                entityManager.getTransaction().rollback();
            } catch (Exception re) {
                log.error("dbTransactions.getObjectsByPropertySorted(" + classname + "," + propertyname + "," + propertyvalue + "): An error occurred attempting to roll back the transaction.", re);
            }
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }

        log.info("dbTransactions.getObjectsByProperty(" + classname + "," + propertyname + "," + propertyvalue + ") returns List<Object>.size:" + ((retlist != null) ? retlist.size() : null));
        return retlist;
    }//EoM getObjectsByProperty

    /**
     * Method getObjectsByManyToOneString(String classname,String
     * manyToOnename,String refidname,String manyToOnevalue,String sortproperty,
     * int sorttype) loads all hibernate Objects from the database after
     * applying a custom where clause related to the property (e.g.
     * "firstname","first1").
     *
     * @param classname     The classname as mapped in the hibernate configuration
     * @param sortproperty  The property that will be used during sorting
     * @param sorttype      If 0 Sorting will be ASC else Sorting will be DESC
     * @return The hibernate List of classes or null
     */
    public static List<Object> getObjectsByManyToOneStringSorted(String classname, String manyToOnename, String
            refidname, String manyToOnevalue, String sortproperty, int sorttype) {
        log.debug("dbTransactions.getObjectsByProperty(" + classname + "+) called");

        //Hibernate Initialisation
        EntityManager entityManager = null;
        //Model Initialisation
        List<Object> retlist = null;
        try {
            entityManager = JPAUtil.getEntityManagerFactory().createEntityManager();
            entityManager.getTransaction().begin();
            String query = "from " + classname + " c where c." + manyToOnename + "." + refidname + "=:param1" + " order by c." + sortproperty + " " + ((sorttype == 0) ? "asc" : "desc");
            //log.debug("dbTransactions.getObjectsByProperty("+classname+","+propertyname+","+propertyvalue+"): Querying: "+query);
            Query q = entityManager.createQuery(query);
            q.setParameter("param1", manyToOnevalue);

            //Perform The Query
            retlist = q.getResultList();

            entityManager.getTransaction().commit();

        } catch (Exception ex) {
            log.error(ex.getMessage());
            log.error("dbTransactions.getObjectsByProperty(" + classname + "): An error occurred during the transaction. Attempting to RollBack", ex);
            try {
                entityManager.getTransaction().rollback();
            } catch (Exception re) {
                log.error("dbTransactions.getObjectsByProperty(" + classname + "): An error occurred attempting to roll back the transaction.", re);
            }
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }

        log.info("dbTransactions.getObjectsByProperty(" + classname + ") returns List<Object>.size:" + ((retlist != null) ? retlist.size() : null));
        return retlist;
    }//EoM getObjectsByProperty

    public static void deleteObjectsBySqlQuery(String classname, String query) {

        Query q;
        EntityManager entityManager = null;
        try {
            entityManager = JPAUtil.getEntityManagerFactory().createEntityManager();
            entityManager.getTransaction().begin();
            String finalQuery = "delete from " + query;
            if (classname == null) {
                finalQuery = "DELETE FROM " + query;
            }

            q = entityManager.createQuery(finalQuery);
            int result = q.executeUpdate();

            if (classname != null) {
                ((NativeQuery) q).addEntity(classname);
            }

            entityManager.getTransaction().commit();


        } catch (Exception ex) {
            log.error("Error in deleteObjectsByCustomQuery: " + ex.getLocalizedMessage());
            try {
                entityManager.getTransaction().rollback();
            } catch (Exception re) {
                log.error("An error occurred attempting to roll back the transaction.", re);
            }
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }
    }

    public static void updateObjectsBySqlQuery(String query) {

        Query q;
        EntityManager entityManager = null;
        try {
            entityManager = JPAUtil.getEntityManagerFactory().createEntityManager();
            entityManager.getTransaction().begin();

            q = entityManager.createQuery(query);
            int result = q.executeUpdate();

            entityManager.getTransaction().commit();


        } catch (Exception ex) {
            log.error("Error in updateObjectsByCustomQuery: " + ex.getLocalizedMessage());
            try {
                entityManager.getTransaction().rollback();
            } catch (Exception re) {
                log.error("An error occurred attempting to roll back the transaction.", re);
            }
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }

    }//EoM UpdateObjectsBySqlQuery

    public static List<Object> getObjectsByPropertyDateNullAndProperty(String classname, String propertyname, String propertyname1, Integer propertyvalue1 ) {
        //log.debug("dbTransactions.getObjectsByProperty("+classname+","+propertyname+","+propertyvalue+") called");

        //Hibernate Initialisation
        //Model Initialisation
        EntityManager entityManager = null;
        List<Object> retlist = null;
        try {
            entityManager = JPAUtil.getEntityManagerFactory().createEntityManager();
            entityManager.getTransaction().begin();


            String query = "from " + classname + " c where c." + propertyname + " IS NULL and c." + propertyname1 + " = " + propertyvalue1;
            log.debug("dbTransactions.getObjectsByProperty(" + classname + "," + propertyname + "," + "): Querying: " + query);
            Query q = entityManager.createQuery(query);

            //Perform The Query
            retlist = q.getResultList();

            entityManager.getTransaction().commit();
        } catch (Exception ex) {
            log.error("dbTransactions.getObjectsByProperty(" + classname + "," + propertyname + "," + "): An error occurred during the transaction. Attempting to RollBack", ex);
            try {
                entityManager.getTransaction().rollback();
            } catch (Exception re) {
                log.error("dbTransactions.getObjectsByProperty(" + classname + "," + propertyname + "," + "): An error occurred attempting to roll back the transaction.", re);
            }
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }

        log.info("dbTransactions.getObjectsByProperty(" + classname + "," + propertyname + "," + ") returns List<Object>.size:" + ((retlist != null) ? retlist.size() : null));
        return retlist;
    }//EoM getObjectsByProperty


    public static Object getSumByProperty(String classname, String propertyName, String propertyKeyname, String propertyKeyValue) {

        EntityManager entityManager = null;
        Object obj = null;
        try {
            entityManager = JPAUtil.getEntityManagerFactory().createEntityManager();
            entityManager.getTransaction().begin();

            Query q = entityManager.createQuery("select sum(" + propertyName + ") from " + classname + " c where c." + propertyKeyname + " = " + propertyKeyValue);
            obj = q.getSingleResult();

            entityManager.getTransaction().commit();

        } catch (Exception ex) {
            log.error("dbTransactions.countAllObjects(" + classname + "): An error occurred during the transaction. Attempting to RollBack", ex);
            try {
                entityManager.getTransaction().rollback();
            } catch (Exception re) {
                log.error("dbTransactions.countAllObjects(" + classname + "): An error occurred attempting to roll back the transaction.", re);
            }
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }

        log.info("dbTransactions.sumObjects(" + classname);

        return obj;
    }

    public static List<Object> getSumGroupByProperty(String classname, String propertyname, String propertygroup) {
        //log.debug("dbTransactions.getObjectsByProperty("+classname+","+propertyname+","+propertyvalue+") called");

        //Hibernate Initialisation
        //Model Initialisation
        EntityManager entityManager = null;
        List<Object> retlist = null;
        try {
            entityManager = JPAUtil.getEntityManagerFactory().createEntityManager();
            entityManager.getTransaction().begin();


            String query = "select sum(" + propertyname + "), " + propertygroup + " from " + classname + " group by " + propertygroup;
            log.debug("dbTransactions.getSumGroupByProperty(" + query);
            Query q = entityManager.createQuery(query);

            //Perform The Query
            retlist = q.getResultList();

            entityManager.getTransaction().commit();
        } catch (Exception ex) {
            log.error("dbTransactions.getObjectsByProperty(" + classname + "," + propertyname + "," + "): An error occurred during the transaction. Attempting to RollBack", ex);
            try {
                entityManager.getTransaction().rollback();
            } catch (Exception re) {
                log.error("dbTransactions.getObjectsByProperty(" + classname + "," + propertyname + "," + "): An error occurred attempting to roll back the transaction.", re);
            }
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }

        log.info("dbTransactions.getObjectsByProperty(" + classname + "," + propertyname + "," + ") returns List<Object>.size:" + ((retlist != null) ? retlist.size() : null));
        return retlist;
    }
}
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
