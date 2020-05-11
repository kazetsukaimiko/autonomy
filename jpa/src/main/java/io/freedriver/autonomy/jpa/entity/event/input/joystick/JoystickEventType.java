package io.freedriver.autonomy.jpa.entity.event.input.joystick;

import io.freedriver.autonomy.jpa.entity.event.input.joystick.jstest.JSTestEvent;
import io.freedriver.autonomy.jpa.entity.event.input.joystick.jstest.JSTestEventType;

import java.util.Objects;
import java.util.stream.Stream;

public enum JoystickEventType {
    BUTTON_DOWN(1L, true),
    BUTTON_UP(0L, true),
    AXIS(Long.MIN_VALUE, false);

    private final Long value;
    private final boolean button;

    JoystickEventType(Long value, boolean button) {
        this.value = value;
        this.button = button;
    }

    public Long getValue() {
        return value;
    }

    public boolean isButton() {
        return button;
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
