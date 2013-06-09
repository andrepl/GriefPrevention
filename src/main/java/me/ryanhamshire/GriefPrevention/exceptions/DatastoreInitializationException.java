package me.ryanhamshire.GriefPrevention.exceptions;

public class DatastoreInitializationException extends Exception {
    public DatastoreInitializationException(String message) {
        super(message);
    }
    public DatastoreInitializationException(Throwable cause) {
        super(cause);
    }
}
