package io.freedriver.autonomy.entity.event.input.joystick;

import io.freedriver.autonomy.entity.event.input.joystick.jstest.JSTestEvent;
import io.freedriver.autonomy.entity.event.input.joystick.jstest.JSTestEventType;

import java.util.Objects;
import java.util.stream.Stream;

public enum JoystickEventType {
    BUTTON_DOWN(1L),
    BUTTON_UP(0L),
    AXIS(Long.MIN_VALUE);

    private final Long value;

    JoystickEventType(Long value) {
        this.value = value;
    }

    public Long getValue() {
        return value;
    }

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

    @Override
    public String toString() {
        return name();
    }
}
