package io.freedriver.autonomy.jaxrs.view;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class AliasView {
    Map<String, Boolean> applianceStates = new LinkedHashMap<>();
    Map<String, Boolean> groupStates = new LinkedHashMap<>();
    Map<String, Set<String>> groups = new LinkedHashMap<>();
    Map<String, Integer> sensors = new LinkedHashMap<>();
    Map<String, Integer> sensorMins = new LinkedHashMap<>();
    Map<String, Integer> sensorMaxes = new LinkedHashMap<>();
    Map<String, Integer> sensorPercentages = new LinkedHashMap<>();

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

    public Map<String, Integer> getSensors() {
        return sensors;
    }

    public void setSensors(Map<String, Integer> sensors) {
        this.sensors = sensors;
    }

    public Map<String, Integer> getSensorMins() {
        return sensorMins;
    }

    public void setSensorMins(Map<String, Integer> sensorMins) {
        this.sensorMins = sensorMins;
    }

    public Map<String, Integer> getSensorMaxes() {
        return sensorMaxes;
    }

    public void setSensorMaxes(Map<String, Integer> sensorMaxes) {
        this.sensorMaxes = sensorMaxes;
    }

    public Map<String, Integer> getSensorPercentages() {
        return sensorPercentages;
    }

    public void setSensorPercentages(Map<String, Integer> sensorPercentages) {
        this.sensorPercentages = sensorPercentages;
    }
}
