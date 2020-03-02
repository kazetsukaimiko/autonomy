package io.freedriver.jsonlink;

import io.freedriver.jsonlink.jackson.schema.v1.Request;
import io.freedriver.jsonlink.jackson.schema.v1.Response;

import java.util.Optional;
import java.util.UUID;

public class ConcurrentConnector implements Connector {
    private final Connector delegate;
    private UUID uuidCache;

    public ConcurrentConnector(Connector delegate) {
        this.delegate = delegate;
    }

    @Override
    public synchronized Response send(Request request) throws ConnectorException {
        return delegate.send(request);
    }

    @Override
    public synchronized UUID getUUID() throws ConnectorException {
        if (uuidCache == null) {
            uuidCache = delegate.getUUID();
        }
        return uuidCache;
    }

    @Override
    public synchronized void processBoardOutput(String line) {
        delegate.processBoardOutput(line);
    }

    @Override
    public synchronized String getStatus() {
        return delegate.getStatus();
    }

    @Override
    public synchronized String device() {
        return delegate.device();
    }

    @Override
    public synchronized boolean isClosed() {
        return delegate.isClosed();
    }

    @Override
    public synchronized void close() throws Exception {
        delegate.close();
    }
}
