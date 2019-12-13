package io.freedriver.autonomy.entity.event.input.joystick;

import io.freedriver.autonomy.entity.event.input.joystick.jstest.JSTestEvent;
import io.freedriver.autonomy.entity.event.input.joystick.jstest.JSTestEventType;

import java.util.Objects;
import java.util.stream.Stream;

public enum JoystickEventType {
    BUTTON_DOWN,
    BUTTON_UP,
    AXIS;

    public static JoystickEventType of(JSTestEvent jsTestEvent) {
        return JSTestEventType.isButton(jsTestEvent.getJsTestEventType()) ?
                jsTestEvent.getValue() == 0L ? BUTTON_UP : BUTTON_DOWN
                :
                AXIS;
    }

    public static JoystickEventType byName(String s) {
        return Stream.of(values())
                .filter(type -> Objects.equals(s, type.name()))
                .findFirst()
                .orElse(null);
    }
}
