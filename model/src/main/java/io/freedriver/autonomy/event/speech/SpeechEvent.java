package io.freedriver.autonomy.event.speech;

import java.time.Instant;

import io.freedriver.autonomy.event.Event;
import io.freedriver.autonomy.event.GenerationOrigin;

public record SpeechEvent(
        String id,
        Instant timestamp,
        GenerationOrigin generationOrigin,
        String sourceClass,
        String sourceId,
        String eventId,
        SpeechEventType speechEventType,
        String subject,
        String text)
        implements Event {}
