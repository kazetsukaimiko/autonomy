package io.freedriver.autonomy.jpa.entity.event.input.joystick;

import io.freedriver.autonomy.jpa.entity.event.Event;
import io.freedriver.autonomy.jpa.entity.event.GenerationOrigin;
import io.freedriver.autonomy.jpa.entity.event.input.joystick.jstest.JSTestEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
public class JoystickEvent extends Event {
    public static final long serialVersionUID = -1L;

    @Column
    private Long number;

    @Column(name = "event_value")
    private Long eventValue;

    @Column
    private boolean initial;

    @Enumerated(EnumType.STRING)
    private JoystickEventType joystickEventType;

    public JoystickEvent(long timestamp, GenerationOrigin generationOrigin, String sourceClass,
                         String sourceId, String eventId, Long number, Long eventValue, boolean initial,
                         JoystickEventType joystickEventType) {
        super(timestamp, generationOrigin, sourceClass, sourceId, eventId);
        this.number = number;
        this.eventValue = eventValue;
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

    public boolean isButton() {
        return joystickEventType.isButton();
    }
}
