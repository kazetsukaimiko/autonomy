package io.freedriver.autonomy.serial;

import java.util.List;
import java.util.UUID;

import io.freedriver.jsonlink.jackson.schema.v1.Request;
import io.freedriver.jsonlink.jackson.schema.v1.Response;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * REST contract between the serial gateway and the controller.
 * The gateway implements it. The controller calls it.
 *
 * <p>A request is a read only when it sets nothing but {@code read}, {@code boardInfo} and
 * {@code requestId}. Every other field is a write: {@code write}, {@code turn_on}, {@code turn_off},
 * {@code mode} and {@code uuid}. A write whose {@value SerialGatewayHeaders#WRITE_TOKEN} header is
 * missing or mismatched is status 403. The credential stays out of {@link SerialGatewayError}.
 * The gateway enforces the header; this module states the rule.
 *
 * <p>Error statuses, each with a {@link SerialGatewayError} body:
 * <ul>
 * <li>404 when no board with that UUID is connected
 * <li>403 when a write lacks a matching write token
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
     * One jsonlink request. {@code writeToken} is null on a read. The return value is the jsonlink
     * response, the same JSON the board speaks.
     */
    @POST
    @Path("/{boardId}/requests")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Response send(
            @PathParam("boardId") UUID boardId,
            Request request,
            @HeaderParam(SerialGatewayHeaders.WRITE_TOKEN) String writeToken);
}
