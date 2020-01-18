package io.freedriver.autonomy.service;

public class ActionInvocationException extends RuntimeException {
    public ActionInvocationException() {
    }

    public ActionInvocationException(String message) {
        super(message);
    }

    public ActionInvocationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ActionInvocationException(Throwable cause) {
        super(cause);
    }

    public ActionInvocationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
