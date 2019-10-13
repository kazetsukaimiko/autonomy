package io.freedriver.controller;

public class JoystickReaderException extends RuntimeException {
    public JoystickReaderException() {
    }

    public JoystickReaderException(String message) {
        super(message);
    }

    public JoystickReaderException(String message, Throwable cause) {
        super(message, cause);
    }

    public JoystickReaderException(Throwable cause) {
        super(cause);
    }

    public JoystickReaderException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
