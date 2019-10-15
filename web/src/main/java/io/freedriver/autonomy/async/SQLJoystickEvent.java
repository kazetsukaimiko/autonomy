package io.freedriver.autonomy.async;

import io.freedriver.autonomy.entity.JoystickEvent;

import java.nio.file.Path;
import java.time.Instant;

public class SQLJoystickEvent extends JoystickEvent {
    private long id;

    public SQLJoystickEvent() {
    }

    public SQLJoystickEvent(long id) {
        this.id = id;
    }

    public SQLJoystickEvent(JoystickEvent joystickEvent, long id) {
        super(joystickEvent);
        this.id = id;
    }

    public SQLJoystickEvent(Path path, Instant timestamp, Type type, Boolean initial, Long number, Long value, long id) {
        super(path, timestamp, type, initial, number, value);
        this.id = id;
    }

}
