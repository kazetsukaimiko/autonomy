package io.freedriver.autonomy.events.store;

import java.time.Instant;
import java.util.UUID;

public record EventRecord<T>(
        String id,
        String eventType,
        Instant timestamp,
        String sourceClass,
        String sourceId,
        String eventId,
        T payload) {

    public static <T> EventRecord<T> of(String eventType, T payload, EventMetadata metadata) {
        return new EventRecord<>(
                UUID.randomUUID().toString(),
                eventType,
                metadata.timestamp(),
                metadata.sourceClass(),
                metadata.sourceId(),
                metadata.eventId(),
                payload);
    }
}