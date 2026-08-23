package io.freedriver.autonomy.events.store;

import java.time.Instant;

public record EventMetadata(
        Instant timestamp,
        String sourceClass,
        String sourceId,
        String eventId) {
}