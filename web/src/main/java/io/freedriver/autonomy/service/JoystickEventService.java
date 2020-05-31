package io.freedriver.autonomy.service;

import io.freedriver.autonomy.jpa.entity.event.input.joystick.JoystickEvent;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import java.io.IOException;
import java.util.logging.Logger;

@ApplicationScoped
public class JoystickEventService extends EventService<JoystickEvent> {
    private static final Logger LOGGER = Logger.getLogger(JoystickEventService.class.getName());

    @Inject
    private EventCoordinateService coordinateService;


    public synchronized void actOnJoystickEvent(@Observes @Default JoystickEvent joystickEvent) throws IOException {
        try {
            persist(joystickEvent);
        } catch (Exception e) {
            LOGGER.warning("Exception persisting joystickEvent: " + e.getClass().getName()+": " + e.getMessage());
        }
    }

    @Override
    public JoystickEvent persist(JoystickEvent event) {


        return super.persist(event);
    }
}
