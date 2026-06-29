package io.freedriver.autonomy.service;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Default;
import jakarta.transaction.Transactional;

import io.freedriver.autonomy.jpa.entity.event.input.joystick.JoystickEvent;
import io.freedriver.autonomy.service.crud.EventCrudService;

@ApplicationScoped
public class JoystickEventCrudService extends EventCrudService<JoystickEvent> {
    private static final Logger LOGGER = Logger.getLogger(JoystickEventCrudService.class.getName());

    @Transactional
    public synchronized void actOnJoystickEvent(@Observes @Default JoystickEvent joystickEvent) throws IOException {
        try {
            persist(joystickEvent);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Exception persisting joystickEvent: " + e.getClass().getName()+": " + e.getMessage());
        }
    }

    @Override
    public Class<JoystickEvent> getEntityClass() {
        return JoystickEvent.class;
    }

}
