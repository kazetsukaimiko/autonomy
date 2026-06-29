package io.freedriver.autonomy.jaxrs.endpoint;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import io.freedriver.autonomy.jpa.entity.event.Event;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

@Path(EventApi.ROOT)
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public interface EventApi<EVENT extends Event> extends ReadApi<EVENT, Long> {
    String ROOT = "/event";
}
