package io.freedriver.autonomy.jaxrs.endpoint.event;

import io.freedriver.autonomy.jaxrs.endpoint.EventApi;
import io.freedriver.autonomy.jpa.entity.event.input.joystick.JoystickEvent;
import io.freedriver.autonomy.service.JoystickEventCrudService;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.xml.datatype.Duration;
import java.util.stream.Stream;

@RequestScoped
public class JoystickEventEndpoint implements EventApi<JoystickEvent> {

    @Inject
    JoystickEventCrudService joystickEventCrudService;

    @Override
    public Stream<JoystickEvent> findAll() {
        return joystickEventCrudService.fromStartOfDay();
    }

    @Override
    public JoystickEvent findOne(Long aLong) {
        return joystickEventCrudService.get(aLong)
                .orElseThrow(() -> new WebApplicationException("Unknown JoystickEvent", 404));
    }
}
