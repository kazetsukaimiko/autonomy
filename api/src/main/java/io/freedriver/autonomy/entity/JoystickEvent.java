package io.freedriver.autonomy.entity;

import io.freedriver.autonomy.jstest.JSMetadata;
import io.freedriver.autonomy.jstest.JSTestEvent;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class JoystickEvent {
    private String title;
    private Instant timestamp;
    private Type type;
    private Boolean initial;
    private String name;
    private Long number;
    private Long value;

    public JoystickEvent() {
    }

    public JoystickEvent(JoystickEvent joystickEvent) {
        this(
                joystickEvent.getTitle(),
                joystickEvent.getTimestamp(),
                joystickEvent.getType(),
                joystickEvent.getInitial(),
                joystickEvent.getName(),
                joystickEvent.getNumber(),
                joystickEvent.getValue()
        );
    }

    public JoystickEvent(String title, Instant timestamp, Type type, Boolean initial, String name, Long number, Long value) {
        this.title = title;
        this.timestamp = timestamp;
        this.type = type;
        this.initial = initial;
        this.name = name;
        this.number = number;
        this.value = value;
    }

    public JoystickEvent(JSTestEvent jsTestEvent) {
        this(
            jsTestEvent.getMetadata().getTitle(),
            jsTestEvent.getNow(),
            Type.of(jsTestEvent),
            JSTestEvent.Type.isInitial(jsTestEvent.getType()),
            jsTestEvent.getMetadata().getNameOf(jsTestEvent),
            jsTestEvent.getNumber(),
            jsTestEvent.getValue()
        );
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JoystickEvent that = (JoystickEvent) o;
        return Objects.equals(title, that.title) &&
                Objects.equals(timestamp, that.timestamp) &&
                type == that.type &&
                Objects.equals(initial, that.initial) &&
                Objects.equals(name, that.name) &&
                Objects.equals(number, that.number) &&
                Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, timestamp, type, initial, name, number, value);
    }

    @Override
    public String toString() {
        return "JoystickEvent{" +
                "title=" + title +
                ", timestamp=" + timestamp +
                ", type=" + type +
                ", initial=" + initial +
                ", name='" + name + '\'' +
                ", number=" + number +
                ", value=" + value +
                '}';
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
