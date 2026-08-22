package io.freedriver.autonomy.events.store;

import java.time.Instant;
import java.util.Optional;

public record EventQuery(
        Optional<String> eventType,
        Optional<Instant> since,
        Optional<String> sourceId,
        Optional<String> id,
        int limit) {

    public static EventQuery since(String eventType, Instant since, int limit) {
        return new EventQuery(
                Optional.of(eventType), Optional.of(since), Optional.empty(), Optional.empty(), limit);
    }

    public static EventQuery all(String eventType) {
        return new EventQuery(
                Optional.of(eventType), Optional.empty(), Optional.empty(), Optional.empty(), Integer.MAX_VALUE);
    }

    public static EventQuery byId(String eventType, String id) {
        return new EventQuery(
                Optional.of(eventType), Optional.empty(), Optional.empty(), Optional.of(id), 1);
    }

    public static EventQuery bySource(String eventType, String sourceId, Instant since, int limit) {
        return new EventQuery(
                Optional.of(eventType),
                Optional.ofNullable(since),
                Optional.of(sourceId),
                Optional.empty(),
                limit);
    }
}
