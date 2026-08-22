package io.freedriver.autonomy.jpa.entity.event.sensor;

import java.time.Instant;

import io.freedriver.autonomy.event.Event;
import io.freedriver.autonomy.event.GenerationOrigin;
import io.freedriver.math.measurement.types.thermo.Temperature;

public record TemperatureEvent(
        String id,
        Instant timestamp,
        GenerationOrigin generationOrigin,
        String sourceClass,
        String sourceId,
        String eventId,
        Temperature temperature)
        implements Event {}
