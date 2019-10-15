package io.freedriver.autonomy.entity;

import io.freedriver.autonomy.jstest.JSTestEvent;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;
import java.util.stream.Stream;

public class JoystickEvent {
    private Path path;
    private Instant timestamp;
    private Type type;
    private Boolean initial;
    private Long number;
    private Long value;

    public JoystickEvent() {
    }

    public JoystickEvent(JoystickEvent joystickEvent) {
        this(
                joystickEvent.getPath(),
                joystickEvent.getTimestamp(),
                joystickEvent.getType(),
                joystickEvent.getInitial(),
                joystickEvent.getNumber(),
                joystickEvent.getValue()
        );
    }

    public JoystickEvent(Path path, Instant timestamp, Type type, Boolean initial, Long number, Long value) {
        this.path = path;
        this.timestamp = timestamp;
        this.type = type;
        this.initial = initial;
        this.number = number;
        this.value = value;
    }

    public JoystickEvent(JSTestEvent jsTestEvent) {
        this(
            jsTestEvent.getPath(),
            jsTestEvent.getNow(),
            Type.of(jsTestEvent),
            JSTestEvent.Type.isInitial(jsTestEvent.getType()),
            jsTestEvent.getNumber(),
            jsTestEvent.getValue()
        );
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Boolean getInitial() {
        return initial;
    }

    public void setInitial(Boolean initial) {
        this.initial = initial;
    }

    public Long getNumber() {
        return number;
    }

    public void setNumber(Long number) {
        this.number = number;
    }

    public Long getValue() {
        return value;
    }

    public void setValue(Long value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "JoystickEvent{" +
                "path=" + path +
                ", timestamp=" + timestamp +
                ", type=" + type +
                ", initial=" + initial +
                ", number=" + number +
                ", value=" + value +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JoystickEvent that = (JoystickEvent) o;
        return Objects.equals(path, that.path) &&
                Objects.equals(timestamp, that.timestamp) &&
                type == that.type &&
                Objects.equals(initial, that.initial) &&
                Objects.equals(number, that.number) &&
                Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, timestamp, type, initial, number, value);
    }

    public enum Type {
        BUTTON_DOWN,
        BUTTON_UP,
        AXIS;

        public static Type of(JSTestEvent jsTestEvent) {
            return JSTestEvent.Type.isButton(jsTestEvent.getType()) ?
                    jsTestEvent.getValue() == 0L ? BUTTON_UP : BUTTON_DOWN
                    :
                    AXIS;
        }


        public static Type byName(String s) {
            return Stream.of(values())
                    .filter(type -> Objects.equals(s, type.name()))
                    .findFirst()
                    .orElse(null);
        }
    }
}
