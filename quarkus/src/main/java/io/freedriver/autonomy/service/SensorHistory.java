package io.freedriver.autonomy.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import lombok.Builder;

@Builder(toBuilder = true)
public record SensorHistory(Map<UUID, BoardAnalogHistory> history) {
    public SensorHistory {
        history = history == null ? new LinkedHashMap<>() : history;
    }

    public SensorHistory() {
        this(new LinkedHashMap<>());
    }
}
