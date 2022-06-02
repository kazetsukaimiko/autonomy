package io.freedriver.autonomy.jaxrs.payload;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class AliasPayload {
    Map<String, Boolean> applianceStates = new LinkedHashMap<>();
    Map<String, Boolean> groupStates = new LinkedHashMap<>();
    /*
    Map<String, Set<String>> groups = new LinkedHashMap<>();
    Map<String, Double> sensors = new LinkedHashMap<>();
    Map<String, Double> sensorMins = new LinkedHashMap<>();
    Map<String, Double> sensorMaxes = new LinkedHashMap<>();
    Map<String, Double> sensorPercentages = new LinkedHashMap<>();

     */

    public AliasPayload() {
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

    /*
    public Map<String, Set<String>> getGroups() {
        return groups;
    }

    public void setGroups(Map<String, Set<String>> groups) {
        this.groups = groups;
    }


    public Map<String, Double> getSensors() {
        return sensors;
    }

    public void setSensors(Map<String, Double> sensors) {
        this.sensors = sensors;
    }

    public Map<String, Double> getSensorMins() {
        return sensorMins;
    }

    public void setSensorMins(Map<String, Double> sensorMins) {
        this.sensorMins = sensorMins;
    }

    public Map<String, Double> getSensorMaxes() {
        return sensorMaxes;
    }

    public void setSensorMaxes(Map<String, Double> sensorMaxes) {
        this.sensorMaxes = sensorMaxes;
    }

    public Map<String, Double> getSensorPercentages() {
        return sensorPercentages;
    }

    public void setSensorPercentages(Map<String, Double> sensorPercentages) {
        this.sensorPercentages = sensorPercentages;
    }

     */
}
