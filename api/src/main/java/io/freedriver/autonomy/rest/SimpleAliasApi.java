package io.freedriver.autonomy.rest;

import javax.ws.rs.*;
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

    @GET
    List<UUID> getBoards();

    @GET
    @Path(BOARD_ID_PATH)
    Map<String, Boolean> getState(@PathParam(BOARD_ID) UUID boardId) throws IOException;

    @GET
    @Path(SETUP_PATH)
    Map<String, Boolean> setupBoard(@PathParam(BOARD_ID) UUID boardId) throws IOException;

    @POST
    @Path(BOARD_ID_PATH)
    Map<String, Boolean> setState(@PathParam(BOARD_ID) UUID boardId, Map<String, Boolean> desiredState) throws IOException;

    default Map<String, Boolean> on(UUID boardId, List<String> pins) throws IOException {
        Map<String, Boolean> state = getState(boardId);
        pins.forEach(pin -> state.put(pin, true));
        return setState(boardId, state);
    }

    default Map<String, Boolean> off(UUID boardId, List<String> pins) throws IOException {
        Map<String, Boolean> state = getState(boardId);
        pins.forEach(pin -> state.put(pin, false));
        return setState(boardId, state);
    }
}
