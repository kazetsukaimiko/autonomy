package io.freedriver.autonomy.service;

import io.freedriver.autonomy.jpa.entity.event.input.joystick.JoystickEvent;
import io.freedriver.autonomy.service.crud.EventCrudService;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Default;
import javax.transaction.Transactional;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

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
