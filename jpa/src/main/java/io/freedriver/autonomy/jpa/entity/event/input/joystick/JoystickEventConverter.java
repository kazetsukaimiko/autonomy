package io.freedriver.autonomy.jpa.entity.event.input.joystick;

import java.time.Instant;
import java.util.UUID;
import java.util.function.BiFunction;

import io.freedriver.autonomy.event.GenerationOrigin;
import io.freedriver.autonomy.event.input.joystick.JoystickEvent;
import io.freedriver.autonomy.event.input.joystick.JoystickEventType;
import io.freedriver.autonomy.event.input.joystick.jstest.JSTestEvent;
import io.freedriver.autonomy.event.input.joystick.jstest.JSTestEventIdLocator;

/**
 * Maps jstest input onto a {@link JoystickEvent}. Lives in jpa so {@link JoystickEvent} need not depend on JSTestEvent.
 */
public final class JoystickEventConverter implements BiFunction<Instant, JSTestEvent, JoystickEvent> {
    public static final JoystickEventConverter INSTANCE = new JoystickEventConverter();

    @Override
    public JoystickEvent apply(Instant timestamp, JSTestEvent jsTestEvent) {
        return new JoystickEvent(
                UUID.randomUUID().toString(),
                timestamp,
                GenerationOrigin.NON_HUMAN,
                JSTestEvent.class.getSimpleName(),
                jsTestEvent.metadata().title(),
                JSTestEventIdLocator.INSTANCE.apply(jsTestEvent),
                jsTestEvent.number(),
                jsTestEvent.value(),
                jsTestEvent.jsTestEventType().isInitial(),
                typeOf(jsTestEvent));
    }

    private static JoystickEventType typeOf(JSTestEvent jsTestEvent) {
        if (!jsTestEvent.jsTestEventType().isButton()) {
            return JoystickEventType.AXIS;
        }
        return jsTestEvent.value() == 0L ? JoystickEventType.BUTTON_UP : JoystickEventType.BUTTON_DOWN;
    }
}
