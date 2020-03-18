package io.freedriver.jsonlink;

import java.time.Duration;

public class ConnectorTimeoutException extends ConnectorException {
    public ConnectorTimeoutException() {
        super();
    }

    public ConnectorTimeoutException(String message) {
        super(message);
    }

    public ConnectorTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConnectorTimeoutException(Throwable cause) {
        super(cause);
    }

    public ConnectorTimeoutException(String description, Duration maxwait) {
        this("Timeout waiting for " + description + ", waited " + maxwait.toMillis() + "ms");
    }
}
