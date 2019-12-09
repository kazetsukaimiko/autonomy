package io.freedriver.wmctrl;

public class WMCtrlException extends RuntimeException {
    public WMCtrlException() {
    }

    public WMCtrlException(String message) {
        super(message);
    }

    public WMCtrlException(String message, Throwable cause) {
        super(message, cause);
    }

    public WMCtrlException(Throwable cause) {
        super(cause);
    }

    public WMCtrlException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
