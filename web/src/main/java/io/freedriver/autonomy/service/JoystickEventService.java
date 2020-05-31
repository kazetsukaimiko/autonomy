package io.freedriver.autonomy.service;

import io.freedriver.autonomy.jpa.entity.event.input.joystick.JoystickEvent;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.Default;
import java.io.IOException;
import java.util.logging.Logger;

public class JoystickEventService extends EventService<JoystickEvent> {
    private static final Logger LOGGER = Logger.getLogger(JoystickEventService.class.getName());

    public synchronized void actOnJoystickEvent(@Observes @Default JoystickEvent joystickEvent) throws IOException {
        try {
            persist(joystickEvent);
        } catch (Exception e) {
            LOGGER.warning("Exception persisting joystickEvent: " + e.getClass().getName()+": " + e.getMessage());
        }
    }
}
