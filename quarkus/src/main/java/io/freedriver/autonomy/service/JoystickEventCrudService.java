package io.freedriver.autonomy.service;

import java.io.IOException;

import io.freedriver.autonomy.event.input.joystick.JoystickEvent;
import io.freedriver.autonomy.events.store.EventTypes;
import io.freedriver.autonomy.service.crud.EventCrudService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Default;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class JoystickEventCrudService extends EventCrudService<JoystickEvent> {

    public synchronized void actOnJoystickEvent(@Observes @Default JoystickEvent joystickEvent) throws IOException {
        try {
            persist(joystickEvent);
        } catch (Exception e) {
            log.warn("Exception persisting joystickEvent: " + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    @Override
    public String eventType() {
        return EventTypes.JOYSTICK_EVENT;
    }

    @Override
    public Class<JoystickEvent> payloadType() {
        return JoystickEvent.class;
    }
}
