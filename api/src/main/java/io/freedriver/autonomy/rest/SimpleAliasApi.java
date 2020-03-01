package io.freedriver.autonomy.rest;

import io.freedriver.jsonlink.jackson.schema.v1.Identifier;

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

    @GET
    List<UUID> getBoards();

    @GET
    @Path(BOARD_ID_PATH)
    Map<Identifier, Boolean> getState(@PathParam(BOARD_ID) UUID boardId) throws IOException;

    @POST
    @Path(BOARD_ID_PATH)
    Map<Identifier, Boolean> setState(@PathParam(BOARD_ID) UUID boardId, Map<Identifier, Boolean> desiredState);

    default Map<Identifier, Boolean> on(UUID boardId, List<Identifier> pins) throws IOException {
        Map<Identifier, Boolean> state = getState(boardId);
        pins.forEach(pin -> state.put(pin, true));
        return setState(boardId, state);
    }

    default Map<Identifier, Boolean> off(UUID boardId, List<Identifier> pins) throws IOException {
        Map<Identifier, Boolean> state = getState(boardId);
        pins.forEach(pin -> state.put(pin, false));
        return setState(boardId, state);
    }
}
