package io.freedriver.autonomy.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class SensorHistory {
    private Map<UUID, BoardAnalogHistory> history = new LinkedHashMap<>();

    public Map<UUID, BoardAnalogHistory> getHistory() {
        return history;
    }

    public void setHistory(Map<UUID, BoardAnalogHistory> history) {
        this.history = history;
    }
}
