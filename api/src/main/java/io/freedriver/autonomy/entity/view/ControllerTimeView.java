package io.freedriver.autonomy.entity.view;

import io.freedriver.autonomy.jpa.entity.VEDirectMessage;
import kaze.victron.StateOfOperation;
import kaze.victron.vedirect.OffReason;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ControllerTimeView {
    private final Map<String, Integer> data;
    private final ChronoUnit unit;
    private final long secondsPerUnit;

    public ControllerTimeView(Map<String, Integer> data, Duration duration) {
        this.data = data;
        //addMissingMapKeys(data);
        if (duration.toSeconds() > 3600) {
            this.unit = ChronoUnit.HOURS;
        } else if (duration.toSeconds() > 60) {
            this.unit = ChronoUnit.MINUTES;
        } else {
            this.unit = ChronoUnit.SECONDS;
        }
        this.secondsPerUnit = unit.getDuration().toSeconds();
    }

    public ControllerTimeView(List<VEDirectMessage> messages, Duration duration) {
        this(messages
                .stream()
                .collect(Collectors.toMap(
                        ControllerTimeView::makeMapKey,
                        m -> 1,
                        Integer::sum)),
                duration);
    }


    public ControllerTimeView(List<VEDirectMessage> messages) {
        this(messages, Duration.between(Instant.now(),
                Instant.ofEpochMilli(messages
                        .stream()
                        .min(VEDirectMessage::orderByTimestamp)
                        .orElseGet(VEDirectMessage::new)
                        .getTimestamp())).abs());
    }


    private static void addMissingMapKeys(Map<String, Integer> data) {
        Stream.of(StateOfOperation.values())
                .forEach(soo -> {
                    if (soo == StateOfOperation.OFF) {
                        Stream.of(OffReason.values())
                                .forEach(offReason -> {
                                    String missingKey = StateOfOperation.OFF + "-" + offReason;
                                    if (!data.containsKey(missingKey)) {
                                        data.put(missingKey, 0);
                                    }
                                });
                    } else if (!data.containsKey(String.valueOf(soo))) {
                        data.put(String.valueOf(soo), 0);
                    }
                });
    }

    public static String makeMapKey(VEDirectMessage message) {
        if (message.getStateOfOperation() != StateOfOperation.OFF) {
            return String.valueOf(message.getStateOfOperation());
        }
        return StateOfOperation.OFF + "-" + message.getOffReason();
    }

    public Map<String, Integer> getData() {
        return data;
    }

    public ChronoUnit getUnit() {
        return unit;
    }

    public long getSecondsPerUnit() {
        return secondsPerUnit;
    }
}
