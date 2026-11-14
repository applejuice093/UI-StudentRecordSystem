package coralcove.util;

/** A rule or validation failure that the user interface can show as-is. */
public class AppException extends RuntimeException {

    public AppException(String message) {
        super(message);
    }

    public AppException(String message, Throwable cause) {
        super(message, cause);
    }
}
