package io.freedriver.jsonlink.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class ConnectorConfigs {
    private Map<UUID, ConnectorConfig> connectors = new HashMap<>();

    public ConnectorConfigs() {
    }

    public Map<UUID, ConnectorConfig> getConnectors() {
        return connectors;
    }

    public void setConnectors(Map<UUID, ConnectorConfig> connectors) {
        this.connectors = connectors;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConnectorConfigs that = (ConnectorConfigs) o;
        return Objects.equals(connectors, that.connectors);
    }

    @Override
    public int hashCode() {
        return Objects.hash(connectors);
    }

    @Override
    public String toString() {
        return "JsonLinkConfigs{" +
                "connectors=" + connectors +
                '}';
    }
}
