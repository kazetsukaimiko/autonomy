package io.freedriver.autonomy.events.store;

public record EventMetadata(
        long timestamp,
        String sourceClass,
        String sourceId,
        String eventId) {
}