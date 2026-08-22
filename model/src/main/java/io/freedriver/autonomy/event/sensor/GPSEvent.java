package io.freedriver.autonomy.event.sensor;

import java.math.BigDecimal;
import java.time.Instant;

import io.freedriver.autonomy.event.Event;
import io.freedriver.autonomy.event.GenerationOrigin;

public record GPSEvent(
        String id,
        Instant timestamp,
        GenerationOrigin generationOrigin,
        String sourceClass,
        String sourceId,
        String eventId,
        BigDecimal latitude,
        BigDecimal longitude)
        implements Event {}
