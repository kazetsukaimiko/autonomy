package io.freedriver.autonomy.jaxrs.endpoint.event;

import java.util.List;

import io.freedriver.autonomy.event.input.joystick.JoystickEvent;
import io.freedriver.autonomy.jaxrs.endpoint.EventApi;
import io.freedriver.autonomy.service.JoystickEventCrudService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;

@RequestScoped
@Path(EventApi.ROOT)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class JoystickEventEndpoint implements EventApi<JoystickEvent> {

    @Inject
    JoystickEventCrudService joystickEventCrudService;

    @Override
    public List<JoystickEvent> findAll() {
        return joystickEventCrudService.fromStartOfDay().toList();
    }

    @Override
    public JoystickEvent findOne(String id) {
        return joystickEventCrudService.get(id)
                .orElseThrow(() -> new WebApplicationException("Unknown JoystickEvent", 404));
    }
}
