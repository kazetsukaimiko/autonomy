package io.freedriver.jsonlink.pin;

import io.freedriver.jsonlink.Connector;
import io.freedriver.jsonlink.ConnectorException;
import io.freedriver.jsonlink.jackson.schema.v1.Identifier;
import io.freedriver.jsonlink.jackson.schema.v1.Mode;
import io.freedriver.jsonlink.jackson.schema.v1.Request;

public abstract class Pin {
    private final Connector connector;
    private final Identifier identifier;
    private final Mode mode;

    public Pin(Connector connector, Identifier identifier, Mode mode) throws ConnectorException {
        this.connector = connector;
        this.identifier = identifier;
        this.mode = mode;
        connector.send(new Request().modeSet(identifier.setMode(mode)));
    }

    public Connector getConnector() {
        return connector;
    }

    public Identifier getIdentifier() {
        return identifier;
    }

    public Mode getMode() {
        return mode;
    }
}
