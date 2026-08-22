package io.freedriver.autonomy.event.input.joystick.jstest;

import java.util.function.Function;

import io.freedriver.autonomy.event.StateType;

/**
 * Derives a stable event id from a {@link JSTestEvent}'s type and number.
 */
public final class JSTestEventIdLocator implements Function<JSTestEvent, String> {
    public static final JSTestEventIdLocator INSTANCE = new JSTestEventIdLocator();

    @Override
    public String apply(JSTestEvent event) {
        JSTestEventType type = event.jsTestEventType();
        return (type.isInitial() ? StateType.INITIAL_STATE : StateType.CHANGE_STATE)
                + "/"
                + ((type.isButton() ? "BUTTON_" : "AXIS_") + event.number());
    }
}
