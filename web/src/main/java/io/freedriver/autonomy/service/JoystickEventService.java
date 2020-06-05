package io.freedriver.autonomy.service;

import io.freedriver.autonomy.jpa.entity.event.input.joystick.JoystickEvent;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Default;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class JoystickEventService extends EventService<JoystickEvent> {
    private static final Logger LOGGER = Logger.getLogger(JoystickEventService.class.getName());

    public synchronized void actOnJoystickEvent(@Observes @Default JoystickEvent joystickEvent) throws IOException {
        try {
            save(joystickEvent);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Exception persisting joystickEvent: " + e.getClass().getName()+": " + e.getMessage(), e);
        }
    }

    @Override
    public Class<JoystickEvent> getEntityClass() {
        return JoystickEvent.class;
    }

    @Override
    public JoystickEvent save(JoystickEvent entity) {
        return persist(entity);
    }

}
