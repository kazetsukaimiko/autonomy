package io.freedriver.jsonlink.config.v2;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Mapping {

    private UUID connectorId;
    private String connectorName;
    private List<Appliance> appliances = new ArrayList<>();

    public Mapping() {
    }

    public UUID getConnectorId() {
        return connectorId;
    }

    public void setConnectorId(UUID connectorId) {
        this.connectorId = connectorId;
    }

    public String getConnectorName() {
        return connectorName;
    }

    public void setConnectorName(String connectorName) {
        this.connectorName = connectorName;
    }

    public List<Appliance> getAppliances() {
        return appliances;
    }

    public void setAppliances(List<Appliance> appliances) {
        this.appliances = appliances;
    }

    @Override
    public String toString() {
        return "Mapping{" +
                "connectorId=" + connectorId +
                ", connectorName='" + connectorName + '\'' +
                ", appliances=" + appliances +
                '}';
    }
}
