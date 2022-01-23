package io.freedriver.autonomy.jpa.entity.event.input.joystick.jstest;

import java.util.Objects;
import java.util.stream.Stream;

public enum JSTestEventType {
    // Value changes
    BUTTON(1),
    AXIS(2),
    // Initial values
    BUTTON_INITIAL(129),
    AXIS_INITIAL(130);

    private final int typeNumber;

    JSTestEventType(int typeNumber) {
        this.typeNumber = typeNumber;
    }

    public static boolean isButton(JSTestEventType JSTestEventType) {
        return JSTestEventType == BUTTON || JSTestEventType == BUTTON_INITIAL;
    }

    public static boolean isInitial(JSTestEventType JSTestEventType) {
        return JSTestEventType == BUTTON_INITIAL || JSTestEventType == AXIS_INITIAL;
    }

    public boolean isInitial() {
        return isInitial(this);
    }

    public int getTypeNumber() {
        return typeNumber;
    }

    public static JSTestEventType ofTypeNumber(long typeNumber) {
        return Stream.of(JSTestEventType.values())
                .filter(JSTestEventType -> typeNumber == JSTestEventType.getTypeNumber())
                .findFirst()
                .orElse(null);
    }

    public static JSTestEventType byName(String s) {
        return Stream.of(JSTestEventType.values())
                .filter(JSTestEventType -> Objects.equals(s, JSTestEventType.name()))
                .findFirst()
                .orElse(null);
    }
}