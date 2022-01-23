package io.freedriver.autonomy.exception;

public class VEDirectApiException extends Exception {
    public VEDirectApiException(String message) {
        super(message);
    }

    public VEDirectApiException(String message, Throwable cause) {
        super(message, cause);
    }

    public VEDirectApiException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public static VEDirectApiException unknownDevice(String serial) {
        return new VEDirectApiException("Unknown Device Serial: " + serial);
    }
}
