package io.freedriver.autonomy.events.store;

import java.time.Instant;
import java.util.Optional;

public record EventQuery(
        Optional<String> eventType,
        Optional<Instant> since,
        Optional<String> sourceId,
        int limit) {

    public static EventQuery since(String eventType, Instant since, int limit) {
        return new EventQuery(Optional.of(eventType), Optional.of(since), Optional.empty(), limit);
    }
}