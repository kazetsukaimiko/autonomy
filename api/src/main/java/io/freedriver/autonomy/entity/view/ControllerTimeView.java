package io.freedriver.autonomy.entity.view;

import kaze.victron.StateOfOperation;
import kaze.victron.vedirect.OffReason;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class ControllerTimeView {
    private final Map<String, Long> data;
    private final ChronoUnit unit;
    private final long secondsPerUnit;

    public ControllerTimeView(Duration duration) {
        if (duration.toSeconds() > 3600) {
            this.unit = ChronoUnit.HOURS;
        } else if (duration.toSeconds() > 60) {
            this.unit = ChronoUnit.MINUTES;
        } else {
            this.unit = ChronoUnit.SECONDS;
        }
        this.secondsPerUnit = unit.getDuration().toSeconds();
        this.data = new LinkedHashMap<>();
    }

    public ControllerTimeView addMissingMapKeys(Set<StateOfOperation> historicalStates,
                                                Set<OffReason> historicalOffReasons) {
        historicalStates
                .forEach(soo -> {
                    if (soo == StateOfOperation.OFF) {
                        historicalOffReasons
                                .forEach(offReason -> {
                                    String missingKey = mapKey(soo, offReason);
                                    if (!data.containsKey(missingKey)) {
                                        data.put(missingKey, 0L);
                                    }
                                });
                    } else if (!data.containsKey(String.valueOf(soo))) {
                        data.put(String.valueOf(soo), 0L);
                    }
                });
        return this;
    }

    public String mapKey(StateOfOperation state, OffReason offReason) {
        return (state != StateOfOperation.OFF)
                ? state.toString()
                : state + "-" + offReason;
    }

    public ControllerTimeView apply(StateOfOperation state, OffReason off, long count) {
        String mapKey = mapKey(state, off);
        if (!data.containsKey(mapKey)) {
            data.put(mapKey, count);
        } else {
            data.put(mapKey, data.get(mapKey)+count);
        }
        return this;
    }

    public Map<String, Long> getData() {
        return data;
    }

    public ChronoUnit getUnit() {
        return unit;
    }

    public long getSecondsPerUnit() {
        return secondsPerUnit;
    }
}
