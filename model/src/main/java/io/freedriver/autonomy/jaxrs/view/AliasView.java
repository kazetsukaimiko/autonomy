package io.freedriver.autonomy.jaxrs.view;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import lombok.Builder;

@Builder(toBuilder = true)
public record AliasView(
        Map<String, Boolean> applianceStates,
        Map<String, Boolean> groupStates,
        Map<String, Set<String>> groups,
        Map<String, Double> sensors,
        Map<String, Double> sensorMins,
        Map<String, Double> sensorMaxes,
        Map<String, Double> sensorPercentages) {

    public AliasView {
        applianceStates = applianceStates == null ? new LinkedHashMap<>() : applianceStates;
        groupStates = groupStates == null ? new LinkedHashMap<>() : groupStates;
        groups = groups == null ? new LinkedHashMap<>() : groups;
        sensors = sensors == null ? new LinkedHashMap<>() : sensors;
        sensorMins = sensorMins == null ? new LinkedHashMap<>() : sensorMins;
        sensorMaxes = sensorMaxes == null ? new LinkedHashMap<>() : sensorMaxes;
        sensorPercentages = sensorPercentages == null ? new LinkedHashMap<>() : sensorPercentages;
    }

    public AliasView() {
        this(null, null, null, null, null, null, null);
    }

    public static AliasView of(
            Map<String, Boolean> applianceStates, Map<String, Boolean> groupStates) {
        return new AliasView(applianceStates, groupStates, null, null, null, null, null);
    }
}
