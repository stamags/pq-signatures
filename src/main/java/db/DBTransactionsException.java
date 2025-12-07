package db;

/**
 * Created by Alex Pyrgiotis on 3/2/2015.
 */
public class DBTransactionsException extends Exception {

    public DBTransactionsException(){}

    public DBTransactionsException(String message){
        super(message);
    }

    public DBTransactionsException(String message, Throwable cause){
        super(message, cause);
    }
}
