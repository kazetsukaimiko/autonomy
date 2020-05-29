package io.freedriver.autonomy.entity.view;

import io.freedriver.autonomy.jpa.entity.VEDirectMessage;
import kaze.victron.StateOfOperation;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ControllerTimeView {
    private final Map<String, Integer> data;
    private final long time;
    private final ChronoUnit unit;

    public ControllerTimeView(Map<String, Integer> data, Duration duration) {
        this.data = data;
        if (duration.toSeconds() > 3600) {
            time = duration.toHours();
            unit = ChronoUnit.HOURS;
        } else if (duration.toSeconds() > 60) {
            time = duration.toMinutes();
            unit = ChronoUnit.MINUTES;
        } else {
            time = duration.toSeconds();
            unit = ChronoUnit.SECONDS;
        }
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

    public static String makeMapKey(VEDirectMessage message) {
        if (message.getStateOfOperation() != StateOfOperation.OFF) {
            return String.valueOf(message.getStateOfOperation());
        }
        return StateOfOperation.OFF + "-" + message.getOffReason();
    }

    public Map<String, Integer> getData() {
        return data;
    }

    public long getTime() {
        return time;
    }

    public ChronoUnit getUnit() {
        return unit;
    }
}
