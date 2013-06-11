package me.ryanhamshire.GriefPrevention.exceptions;

public class InvalidFlagException extends Throwable {
    public InvalidFlagException(Throwable cause) {
        super(cause);
    }

    public InvalidFlagException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidFlagException(String message) {
        super(message);
    }
}
