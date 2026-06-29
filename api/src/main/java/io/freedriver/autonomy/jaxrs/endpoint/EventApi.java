package io.freedriver.autonomy.jaxrs.endpoint;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import io.freedriver.autonomy.jpa.entity.event.Event;

@Path(EventApi.ROOT)
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public interface EventApi<EVENT extends Event> extends ReadApi<EVENT, Long> {
    String ROOT = "/event";
}
