package io.freedriver.autonomy.entity.event.input.joystick.jstest;

public class JSTestException extends RuntimeException {
    public JSTestException() {
    }

    public JSTestException(String message) {
        super(message);
    }

    public JSTestException(String message, Throwable cause) {
        super(message, cause);
    }

    public JSTestException(Throwable cause) {
        super(cause);
    }

    public JSTestException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
