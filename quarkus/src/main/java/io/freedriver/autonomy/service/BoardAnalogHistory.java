package io.freedriver.autonomy.service;

import io.freedriver.jsonlink.jackson.schema.v1.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;

public class BoardAnalogHistory {
    private Map<Identifier, Integer> minimums = new LinkedHashMap<>();
    private Map<Identifier, Integer> maximums = new LinkedHashMap<>();
    private Map<Identifier, Integer> lastKnowns = new LinkedHashMap<>();

    public Map<Identifier, Integer> getMinimums() {
        return minimums;
    }

    public void setMinimums(Map<Identifier, Integer> minimums) {
        this.minimums = minimums;
    }

    public Map<Identifier, Integer> getMaximums() {
        return maximums;
    }

    public void setMaximums(Map<Identifier, Integer> maximums) {
        this.maximums = maximums;
    }

    public Map<Identifier, Integer> getLastKnowns() {
        return lastKnowns;
    }

    public void setLastKnowns(Map<Identifier, Integer> lastKnowns) {
        this.lastKnowns = lastKnowns;
    }
}
