package io.freedriver.autonomy.entity.event.input.joystick.jstest;

import io.freedriver.autonomy.entity.event.EventCoordinate;
import io.freedriver.autonomy.entity.event.EventDescription;
import io.freedriver.autonomy.entity.event.StateType;
import io.freedriver.autonomy.entity.event.input.joystick.JoystickEventType;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Container for Joystick Event Data.
 */
public class JSTestEvent {
    private JSMetadata metadata;
    private Instant now;
    private JSTestEventType jsTestEventType;
    private Long time;
    private Long number;
    private Long value;

    public JSTestEvent() {
    }

    public JSTestEvent(JSMetadata metadata, JSTestEventType jsTestEventType, Long time, Long number, Long value) {
        this.metadata = metadata;
        this.now = Instant.now();
        this.jsTestEventType = jsTestEventType;
        this.time = time;
        this.number = number;
        this.value = value;
    }

    private JSTestEvent(JSMetadata joystickMetadata, Map<String, Long> jstestEventMap) {
        this(
                joystickMetadata,
                JSTestEventType.ofTypeNumber(jstestEventMap.get("type")),
                jstestEventMap.get("time"),
                jstestEventMap.get("number"),
                jstestEventMap.get("value")
        );
    }

    public JSTestEvent(JSMetadata joystickMetadata, String jstestEvent) {
        this(joystickMetadata, jstestEventMap(jstestEvent));
    }

    /**
     * Predicate method to detect valid joystick events.
     */
    public static boolean validEvent(String jstestEvent) {
        return jstestEvent != null &&
                jstestEvent.startsWith("Event: ");
    }

    /**
     * Reads a jstest event string into a map.
     */
    private static Map<String, Long> jstestEventMap(String jstestEvent) {
        return Stream.of(jstestEvent)
                .filter(JSTestEvent::validEvent)
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

    public JSMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(JSMetadata metadata) {
        this.metadata = metadata;
    }

    public Instant getNow() {
        return now;
    }

    public void setNow(Instant now) {
        this.now = now;
    }

    public JSTestEventType getJsTestEventType() {
        return jsTestEventType;
    }

    public void setJsTestEventType(JSTestEventType jsTestEventType) {
        this.jsTestEventType = jsTestEventType;
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
        return "JSTestEvent{" +
                "metadata=" + metadata +
                ", now=" + now +
                ", type=" + jsTestEventType +
                ", time=" + time +
                ", number=" + number +
                ", value=" + value +
                '}';
    }

    /**
     * Returns the source of the event/
     */
    public EventCoordinate locate() {
        return new EventCoordinate(
                getMetadata().getTitle(),
                (JSTestEventType.isButton(getJsTestEventType()) ?
                        "BUTTON_" : "AXIS_") + getNumber()
        );
    }

    /**
     * Returns a description of the event.
     */
    public EventDescription describe() {
        return new EventDescription(
                JSTestEventType.isInitial(getJsTestEventType()) ?
                        StateType.INITIAL_STATE : StateType.CHANGE_STATE,
                JoystickEventType.of(this)
        );
    }

}
