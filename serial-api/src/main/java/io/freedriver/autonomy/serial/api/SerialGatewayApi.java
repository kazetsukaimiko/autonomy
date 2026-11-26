package io.freedriver.autonomy.serial.api;

import java.util.List;
import java.util.UUID;

import io.freedriver.jsonlink.api.v1.BoardRequest;
import io.freedriver.jsonlink.api.v1.BoardResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * REST contract between the serial gateway and the controller.
 * The gateway implements it. The controller calls it.
 *
 * <p>There is no write token and no 403. The gateway's only boundary is binding to
 * 127.0.0.1. This module states the rule; the gateway enforces the bind.
 *
 * <p>Error statuses, each with a {@link SerialGatewayError} body and no board data:
 * <ul>
 * <li>404 when no board with that UUID is connected
 * <li>503 when the board is reconnecting
 * <li>504 when the board does not answer within the request timeout (default 5 seconds)
 * </ul>
 */
@Path("/boards")
public interface SerialGatewayApi {

    /**
     * Every board with a completed UUID handshake.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<BoardView> boards();

    /**
     * One jsonlink request. The return value is the board's reply. The request body is the last
     * parameter.
     */
    @POST
    @Path("/{boardId}/requests")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    BoardResponse send(@PathParam("boardId") UUID boardId, BoardRequest request);
}
