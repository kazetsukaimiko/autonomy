package io.freedriver.autonomy.event.input.sensors;

import java.time.Instant;
import java.util.UUID;

import io.freedriver.autonomy.event.GenerationOrigin;

public record DoubleValueSensorEvent(
        String id,
        Instant timestamp,
        GenerationOrigin generationOrigin,
        String sourceClass,
        String sourceId,
        String eventId,
        UUID boardId,
        String sensorName,
        double eventValue)
        implements SensorEvent {}
