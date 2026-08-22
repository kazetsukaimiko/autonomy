package io.freedriver.autonomy.jpa.entity.event.input.joystick;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import io.freedriver.autonomy.entity.EntityBase;
import io.freedriver.autonomy.event.Event;
import io.freedriver.autonomy.event.GenerationOrigin;
import io.freedriver.autonomy.event.input.joystick.JoystickEvent;
import io.freedriver.autonomy.event.input.joystick.JoystickEventType;
import org.junit.jupiter.api.Test;

class JoystickEventTest {

    @Test
    void isAFinalEventRecord() {
        JoystickEvent event = new JoystickEvent(
                "id-1",
                Instant.EPOCH,
                GenerationOrigin.NON_HUMAN,
                "test",
                "src",
                "evt",
                11L,
                0L,
                false,
                JoystickEventType.BUTTON_UP);

        assertInstanceOf(Event.class, event);
        assertInstanceOf(EntityBase.class, event);
        assertEquals("id-1", event.id());
        assertEquals(11L, event.number());
        assertEquals(0L, event.eventValue());
        assertFalse(event.initial());
        assertTrue(event.joystickEventType().isButton());
    }
}
