package io.freedriver.autonomy.rest;

import io.freedriver.autonomy.entity.jsonlink.WorkspaceEntity;
import org.dizitart.no2.NitriteId;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.stream.Stream;

@Path(WorkspaceEndpointApi.ROOT)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface WorkspaceEndpointApi {
    String ROOT = "/workspaces";
    String WORKSPACE_ID = "workspaceId";
    String BY_ID = "/id/{"+WORKSPACE_ID+"}";
    String NEW_PATH = "/new";

    @GET
    Stream<WorkspaceEntity> getWorkspaces();

    @GET
    @Path(BY_ID)
    WorkspaceEntity getWorkspaceById(@PathParam(BY_ID) NitriteId id);

    @POST
    @Path(NEW_PATH)
    WorkspaceEntity createWorkspace();

    @PUT
    @Path(BY_ID)
    WorkspaceEntity update(@PathParam(BY_ID) NitriteId id, WorkspaceEntity workspaceEntity);

    @DELETE
    @Path(BY_ID)
    boolean delete(@PathParam(BY_ID) NitriteId id);
}