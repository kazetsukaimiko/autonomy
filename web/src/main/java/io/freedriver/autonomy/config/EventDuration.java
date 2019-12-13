package io.freedriver.autonomy.config;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.stream.Stream;

public enum EventDuration {
    INSTANT(0L),
    SHORT(250L),
    MIDDLE(500L),
    LONG(750L),
    VOID(Long.MAX_VALUE)
    ;

    private final Duration maximum;

    EventDuration(long millis) {
        this(Duration.of(millis, ChronoUnit.MILLIS));
    }

    EventDuration(Duration duration) {
        this.maximum = duration;
    }

    public Duration getMaximum() {
        return maximum;
    }

    public static EventDuration get(long millis) {
        return get(Duration.of(millis, ChronoUnit.MILLIS));
    }

    public static EventDuration get(Duration input) {
        return Stream.of(EventDuration.values())
                .filter(eventDuration -> input.compareTo(eventDuration.getMaximum()) <= 0)
                .max(Sorter.INSTANCE)
                .orElse(VOID);
    }

    private static final class Sorter implements Comparator<EventDuration> {
        static final Sorter INSTANCE = new Sorter();

        private Sorter() {
        }

        @Override
        public int compare(EventDuration eventDuration, EventDuration t1) {
            return eventDuration.getMaximum().compareTo(t1.getMaximum());
        }
    }
}
