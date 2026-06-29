package io.freedriver.autonomy.jaxrs.endpoint;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.freedriver.autonomy.jaxrs.view.AliasView;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path(SimpleAliasApi.ROOT)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface SimpleAliasApi {
    String ROOT = "/simple";
    String BOARD_ID = "board";
    String BOARD_ID_PATH = "/id/{"+BOARD_ID+"}";
    String SETUP_PATH = BOARD_ID_PATH + "/setup";
    String GROUP_NAME = "group";
    String GROUP_PATH = BOARD_ID_PATH + "/group/{"+GROUP_NAME+"}";

    @GET
    List<UUID> getBoards();

    @GET
    @Path(BOARD_ID_PATH)
    AliasView getState(@PathParam(BOARD_ID) UUID boardId) throws IOException;

    @GET
    @Path(SETUP_PATH)
    AliasView setupBoard(@PathParam(BOARD_ID) UUID boardId) throws IOException;

    @POST
    @Path(BOARD_ID_PATH)
    AliasView setState(@PathParam(BOARD_ID) UUID boardId, Map<String, Boolean> desiredState) throws IOException;

    @POST
    @Path(GROUP_PATH)
    AliasView setGroup(@PathParam(BOARD_ID) UUID boardId, @PathParam(GROUP_NAME) String group, boolean desiredState) throws IOException;
}
