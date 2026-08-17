package io.freedriver.autonomy.entity.view;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.freedriver.victron.StateOfOperation;
import io.freedriver.victron.vedirect.OffReason;
import lombok.Builder;

@Builder(toBuilder = true)
public record ControllerTimeView(
        Map<String, Long> data,
        ChronoUnit unit,
        long secondsPerUnit) {

    public ControllerTimeView {
        data = data == null ? new LinkedHashMap<>() : data;
    }

    public ControllerTimeView(Duration duration) {
        this(new LinkedHashMap<>(), unitFor(duration), unitFor(duration).getDuration().toSeconds());
    }

    private static ChronoUnit unitFor(Duration duration) {
        if (duration.toSeconds() > 3600) {
            return ChronoUnit.HOURS;
        } else if (duration.toSeconds() > 60) {
            return ChronoUnit.MINUTES;
        }
        return ChronoUnit.SECONDS;
    }

    public ControllerTimeView addMissingMapKeys(Set<StateOfOperation> historicalStates,
                                                Set<OffReason> historicalOffReasons) {
        historicalStates
                .forEach(soo -> {
                    if (soo == StateOfOperation.OFF) {
                        historicalOffReasons
                                .forEach(offReason -> {
                                    Optional<String> key = mapKey(soo, offReason);
                                    if (key.isPresent()) {
                                        String missingKey = key.get();
                                        if (!data.containsKey(missingKey)) {
                                            data.put(missingKey, 0L);
                                        }
                                    }
                                });
                    } else if (!data.containsKey(String.valueOf(soo))) {
                        data.put(String.valueOf(soo), 0L);
                    }
                });
        return this;
    }

    public Optional<String> mapKey(StateOfOperation state, OffReason offReason) {
        return Optional.ofNullable(state)
            .map(s -> (s != StateOfOperation.OFF)
                ? s.toString()
                : s + "-" + offReason);
    }

    public ControllerTimeView apply(StateOfOperation state, OffReason off, long count) {
        Optional<String> key = mapKey(state, off);
        if (key.isPresent()) {
            String mapKey = key.get();
            if (!data.containsKey(mapKey)) {
                data.put(mapKey, count);
            } else {
                data.put(mapKey, data.get(mapKey) + count);
            }
        }
        return this;
    }
}
