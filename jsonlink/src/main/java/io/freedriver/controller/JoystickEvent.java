package io.freedriver.controller;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JoystickEvent {
    private Path path;
    private Instant now;
    private Type type;
    private Long time;
    private Long number;
    private Long value;

    public JoystickEvent() {
    }

    public JoystickEvent(Path path, Type type, Long time, Long number, Long value) {
        this.path = path;
        this.now = Instant.now();
        this.type = type;
        this.time = time;
        this.number = number;
        this.value = value;
    }

    private JoystickEvent(Path joystickPath, Map<String, Long> jstestEventMap) {
        this(
                joystickPath,
                Type.ofTypeNumber(jstestEventMap.get("type")),
                jstestEventMap.get("time"),
                jstestEventMap.get("number"),
                jstestEventMap.get("value")
        );
    }

    public JoystickEvent(Path joystickPath, String jstestEvent) {
        this(joystickPath, jstestEventMap(jstestEvent));
    }

    public static boolean validEvent(String jstestEvent) {
        return jstestEvent != null &&
                jstestEvent.startsWith("Event: ");
    }

    private static Map<String, Long> jstestEventMap(String jstestEvent) {
        return Stream.of(jstestEvent)
                .filter(JoystickEvent::validEvent)
                .map(eventLine -> eventLine.split("Event: "))
                .filter(eventLine -> eventLine.length == 2)
                .map(eventLine -> eventLine[1])
                .map(eventContent -> eventContent.split("\\s*,\\s* "))
                .filter(eventPairs -> eventPairs.length == 4)
                .flatMap(Stream::of)
                .map(kvpair -> kvpair.split("\\s+"))
                .collect(Collectors.toMap(
                        kvpair -> kvpair[0],
                        kvpair -> Long.parseLong(kvpair[1])));
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public Instant getNow() {
        return now;
    }

    public void setNow(Instant now) {
        this.now = now;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
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
                ", now=" + now +
                ", type=" + type +
                ", time=" + time +
                ", number=" + number +
                ", value=" + value +
                '}';
    }

    public enum Type {
        BUTTON(1),
        AXIS(2),
        BUTTON_INITIAL(129),
        AXIS_INITIAL(130);

        private final int typeNumber;

        Type(int typeNumber) {
            this.typeNumber = typeNumber;
        }

        public int getTypeNumber() {
            return typeNumber;
        }

        public static Type ofTypeNumber(long typeNumber) {
            return Stream.of(Type.values())
                    .filter(type -> typeNumber == type.getTypeNumber())
                    .findFirst()
                    .orElse(null);
        }

        public static Type byName(String s) {
            return Stream.of(Type.values())
                    .filter(type -> Objects.equals(s, type.name()))
                    .findFirst()
                    .orElse(null);
        }
    }
}
