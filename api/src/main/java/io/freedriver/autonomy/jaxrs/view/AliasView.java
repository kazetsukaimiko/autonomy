package io.freedriver.autonomy.jaxrs.view;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class AliasView {
    Map<String, Boolean> applianceStates = new LinkedHashMap<>();
    Map<String, Boolean> groupStates = new LinkedHashMap<>();
    Map<String, Set<String>> groups = new LinkedHashMap<>();
    Map<String, Float> sensors = new LinkedHashMap<>();
    Map<String, Float> sensorMins = new LinkedHashMap<>();
    Map<String, Float> sensorMaxes = new LinkedHashMap<>();
    Map<String, Float> sensorPercentages = new LinkedHashMap<>();

    public AliasView() {
    }

    public Map<String, Boolean> getApplianceStates() {
        return applianceStates;
    }

    public void setApplianceStates(Map<String, Boolean> applianceStates) {
        this.applianceStates = applianceStates;
    }

    public Map<String, Boolean> getGroupStates() {
        return groupStates;
    }

    public void setGroupStates(Map<String, Boolean> groupStates) {
        this.groupStates = groupStates;
    }

    public Map<String, Set<String>> getGroups() {
        return groups;
    }

    public void setGroups(Map<String, Set<String>> groups) {
        this.groups = groups;
    }

    public Map<String, Float> getSensors() {
        return sensors;
    }

    public void setSensors(Map<String, Float> sensors) {
        this.sensors = sensors;
    }

    public Map<String, Float> getSensorMins() {
        return sensorMins;
    }

    public void setSensorMins(Map<String, Float> sensorMins) {
        this.sensorMins = sensorMins;
    }

    public Map<String, Float> getSensorMaxes() {
        return sensorMaxes;
    }

    public void setSensorMaxes(Map<String, Float> sensorMaxes) {
        this.sensorMaxes = sensorMaxes;
    }

    public Map<String, Float> getSensorPercentages() {
        return sensorPercentages;
    }

    public void setSensorPercentages(Map<String, Float> sensorPercentages) {
        this.sensorPercentages = sensorPercentages;
    }
}
