package io.freedriver.autonomy.rest;

import io.freedriver.autonomy.rest.view.AliasView;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
