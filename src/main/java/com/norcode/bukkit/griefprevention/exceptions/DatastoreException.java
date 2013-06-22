package com.norcode.bukkit.griefprevention.exceptions;

public class DatastoreException extends Exception {
    public DatastoreException(String message) {
        super(message);
    }
    public DatastoreException(Throwable cause) {
        super(cause);
    }
}
