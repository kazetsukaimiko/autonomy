package io.freedriver.autonomy.events.store;

import java.time.Instant;
import java.util.Map;

public record StoredEvent(
        String id,
        String eventType,
        Instant timestamp,
        String sourceClass,
        String sourceId,
        String eventId,
        Map<String, Object> payload) {
}