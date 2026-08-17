package io.freedriver.autonomy.jpa.entity.event.input.joystick.jstest;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.freedriver.autonomy.jpa.entity.event.StateType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Container for Joystick Event Data.
 */
@Getter
@Setter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
public class JSTestEvent {
    private JSMetadata metadata;
    private Instant now;
    private JSTestEventType jsTestEventType;
    private Long time;
    private Long number;
    private Long value;

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

    public boolean isButton() {
        return Optional.ofNullable(jsTestEventType)
                .map(JSTestEventType::isButton)
                .orElse(false);
    }

    public boolean isAxis() {
        return !isButton();
    }

    public String locateSourceId() {
        return getMetadata().getTitle();
    }

    /**
     * Returns the source of the event/
     */
    public String locateEventId() {
        return (JSTestEventType.isInitial(getJsTestEventType()) ?
                StateType.INITIAL_STATE : StateType.CHANGE_STATE) + "/" +
                ((JSTestEventType.isButton(getJsTestEventType()) ?
                        "BUTTON_" : "AXIS_") + getNumber());
    }
}
