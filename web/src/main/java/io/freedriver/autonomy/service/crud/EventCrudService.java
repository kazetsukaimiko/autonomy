package io.freedriver.autonomy.service.crud;

import io.freedriver.autonomy.jpa.entity.event.Event;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;


public abstract class EventCrudService<E extends Event> extends JPACrudService<E> {
    public static Instant getStartOfDay() {
        LocalDateTime localDateTime = LocalDateTime.now().toLocalDate().atStartOfDay();
        return localDateTime.toInstant(ZoneOffset.UTC);
    }

    public static Instant ago(Duration duration) {
        return LocalDateTime
                .now()
                .minus(duration)
                .toInstant(ZoneOffset.UTC);
    }
}
