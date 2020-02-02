package io.freedriver.autonomy.entity.event.input.joystick;

import io.freedriver.autonomy.entity.event.Event;
import io.freedriver.autonomy.entity.event.EventCoordinate;
import io.freedriver.autonomy.entity.event.EventDescription;
import io.freedriver.autonomy.entity.event.SourceType;
import io.freedriver.autonomy.entity.event.input.joystick.jstest.JSTestEvent;
import org.dizitart.no2.objects.InheritIndices;

import java.time.Instant;

@InheritIndices
public class JoystickEvent extends Event {
    private Long number;
    private Long value;
    private JoystickEventType joystickEventType;

    public JoystickEvent() {
    }

    public JoystickEvent(Instant timestamp, EventCoordinate coordinate, EventDescription description, Long number, Long value, JoystickEventType joystickEventType) {
        super(timestamp, coordinate, description, SourceType.HUMAN);
        this.number = number;
        this.value = value;
        this.joystickEventType = joystickEventType;
    }

    public JoystickEvent(Instant timestamp, JSTestEvent jsTestEvent) {
        this(
                timestamp,
                jsTestEvent.locate(),
                jsTestEvent.describe(),
                jsTestEvent.getNumber(),
                jsTestEvent.getValue(),
                JoystickEventType.of(jsTestEvent)
        );
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

    public JoystickEventType getJoystickEventType() {
        return joystickEventType;
    }

    public void setJoystickEventType(JoystickEventType joystickEventType) {
        this.joystickEventType = joystickEventType;
    }
}
