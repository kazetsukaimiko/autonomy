package io.freedriver.jsonlink.config.v2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Mapping {

    private UUID connectorId;
    private String connectorName;
    private List<Appliance> appliances = new ArrayList<>();
    private Map<String, List<String>> controlMap = new HashMap<>();

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

    public Map<String, List<String>> getControlMap() {
        return controlMap;
    }

    public void setControlMap(Map<String, List<String>> controlMap) {
        this.controlMap = controlMap;
    }

    @Override
    public String toString() {
        return "Mapping{" +
                "connectorId=" + connectorId +
                ", connectorName='" + connectorName + '\'' +
                ", appliances=" + appliances +
                ", controlMap=" + controlMap +
                '}';
    }
}
