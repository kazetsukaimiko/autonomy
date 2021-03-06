package io.freedriver.autonomy.jpa.entity.event.input.joystick;

import io.freedriver.autonomy.jpa.entity.event.Event;
import io.freedriver.autonomy.jpa.entity.event.GenerationOrigin;
import io.freedriver.autonomy.jpa.entity.event.input.joystick.jstest.JSTestEvent;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table
@Inheritance(strategy = InheritanceType.JOINED)
public class JoystickEvent extends Event {
    public static final long serialVersionUID = -1L;

    @Column
    private Long number;

    @Column
    private Long value;

    @Column
    private boolean initial;

    @Enumerated(EnumType.STRING)
    private JoystickEventType joystickEventType;

    public JoystickEvent() {
    }

    public JoystickEvent(long timestamp, GenerationOrigin generationOrigin, String sourceClass,
                         String sourceId, String eventId, Long number, Long value, boolean initial,
                         JoystickEventType joystickEventType) {
        super(timestamp, generationOrigin, sourceClass, sourceId, eventId);
        this.number = number;
        this.value = value;
        this.initial = initial;
        this.joystickEventType = joystickEventType;
    }

    public JoystickEvent(long timestamp, JSTestEvent jsTestEvent) {
        this(
                timestamp,
                GenerationOrigin.NON_HUMAN,
                JSTestEvent.class.getSimpleName(),
                jsTestEvent.locateSourceId(),
                jsTestEvent.locateEventId(),
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
