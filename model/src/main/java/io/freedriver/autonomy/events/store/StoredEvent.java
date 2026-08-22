package io.freedriver.autonomy.events.store;

import java.util.Map;

public record StoredEvent(
        String id,
        String eventType,
        long timestamp,
        String sourceClass,
        String sourceId,
        String eventId,
        Map<String, Object> payload) {
}