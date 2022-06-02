package io.freedriver.autonomy.deployments.common.api;

import io.freedriver.jsonlink.jackson.schema.v1.Request;
import io.freedriver.jsonlink.jackson.schema.v1.Response;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.UUID;

// TODO : Move to its own maven module.
@Path("/jsonlink")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface JsonlinkApi {
    String CONNECTOR_ID = "connectorId";

    @GET
    List<UUID> getBoards();

    @POST
    @Path("/{" + CONNECTOR_ID + "}")
    Response send(@PathParam(CONNECTOR_ID) UUID connectorId, Request request);
}
