package io.freedriver.jsonlink;

import java.io.IOException;

public class ConnectorException extends IOException {
    public ConnectorException() {
    }

    public ConnectorException(String message) {
        super(message);
    }

    public ConnectorException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConnectorException(Throwable cause) {
        super(cause);
    }
}
