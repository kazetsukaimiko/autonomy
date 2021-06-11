package io.freedriver.autonomy.jaxrs.endpoint;

import io.freedriver.autonomy.jpa.entity.event.Event;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path(EventApi.ROOT)
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public interface EventApi<EVENT extends Event> extends ReadApi<EVENT, Long> {
    String ROOT = "/event";
}
