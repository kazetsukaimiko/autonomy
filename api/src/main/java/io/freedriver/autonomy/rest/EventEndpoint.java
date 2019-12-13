package io.freedriver.autonomy.rest;

import io.freedriver.autonomy.entity.event.Event;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path(EventEndpoint.ROOT)
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public interface EventEndpoint<ID> extends CRUDEndpoint<Event, ID> {
    public static final String ROOT = "/event";
}
