package io.freedriver.autonomy.service;

import java.util.LinkedHashMap;
import java.util.Map;

import io.freedriver.jsonlink.jackson.schema.v1.Identifier;
import lombok.Builder;

@Builder(toBuilder = true)
public record BoardAnalogHistory(
        Map<Identifier, Integer> minimums,
        Map<Identifier, Integer> maximums,
        Map<Identifier, Integer> lastKnowns) {

    public BoardAnalogHistory {
        minimums = minimums == null ? new LinkedHashMap<>() : minimums;
        maximums = maximums == null ? new LinkedHashMap<>() : maximums;
        lastKnowns = lastKnowns == null ? new LinkedHashMap<>() : lastKnowns;
    }

    public BoardAnalogHistory() {
        this(new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>());
    }
}
