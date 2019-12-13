package io.freedriver.autonomy.rest;

import io.freedriver.autonomy.service.ConnectorService;
import io.freedriver.jsonlink.jackson.schema.v1.Response;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

@Path("connector")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ConnectorEndpoint {

    public static final String GROUP_PARAMETER = "group";

    @Inject
    private ConnectorService connectorService;

    @GET
    @Path("/group/{"+GROUP_PARAMETER+"}")
    public Response cycleGroup(@PathParam(GROUP_PARAMETER) String groupName) {
        return connectorService.cyclePinGroup(groupName)
                .orElseThrow(() -> new WebApplicationException("Unknown group " + groupName, 404));
    }
}
