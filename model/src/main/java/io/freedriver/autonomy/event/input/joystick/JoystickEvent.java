package io.freedriver.autonomy.event.input.joystick;

import java.time.Instant;

import io.freedriver.autonomy.event.Event;
import io.freedriver.autonomy.event.GenerationOrigin;

public record JoystickEvent(
        String id,
        Instant timestamp,
        GenerationOrigin generationOrigin,
        String sourceClass,
        String sourceId,
        String eventId,
        Long number,
        Long eventValue,
        boolean initial,
        JoystickEventType joystickEventType)
        implements Event {}
