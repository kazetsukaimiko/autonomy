package io.freedriver.autonomy.entity.event.input.joystick;

import io.freedriver.autonomy.entity.event.Event;
import io.freedriver.autonomy.entity.event.EventCoordinate;
import io.freedriver.autonomy.entity.event.EventDescription;
import io.freedriver.autonomy.entity.event.SourceType;
import io.freedriver.autonomy.entity.event.input.joystick.jstest.JSTestEvent;
import org.dizitart.no2.objects.InheritIndices;

import java.time.Instant;
import java.util.Objects;

@InheritIndices
public class JoystickEvent extends Event {
    private Long number;
    private Long value;
    private boolean initial;
    private JoystickEventType joystickEventType;

    public JoystickEvent() {
    }

    public JoystickEvent(Instant timestamp, EventCoordinate coordinate, EventDescription description, Long number, Long value, boolean initial, JoystickEventType joystickEventType) {
        super(timestamp, coordinate, description, SourceType.HUMAN);
        this.number = number;
        this.value = value;
        this.initial = initial;
        this.joystickEventType = joystickEventType;
    }

    public JoystickEvent(Instant timestamp, JSTestEvent jsTestEvent) {
        this(
                timestamp,
                jsTestEvent.locate(),
                jsTestEvent.describe(),
                jsTestEvent.getNumber(),
                jsTestEvent.getValue(),
                jsTestEvent.getJsTestEventType().isInitial(),
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

    public boolean isInitial() {
        return initial;
    }

    public void setInitial(boolean initial) {
        this.initial = initial;
    }

    public JoystickEventType getJoystickEventType() {
        return joystickEventType;
    }

    public void setJoystickEventType(JoystickEventType joystickEventType) {
        this.joystickEventType = joystickEventType;
    }

    public boolean isButton() {
        return joystickEventType.isButton();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        JoystickEvent that = (JoystickEvent) o;
        return initial == that.initial &&
                Objects.equals(number, that.number) &&
                Objects.equals(value, that.value) &&
                joystickEventType == that.joystickEventType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), number, value, initial, joystickEventType);
    }
}
