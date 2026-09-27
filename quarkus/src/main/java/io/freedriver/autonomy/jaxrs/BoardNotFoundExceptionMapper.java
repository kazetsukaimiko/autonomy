package io.freedriver.autonomy.jaxrs;

import io.freedriver.autonomy.service.BoardNotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * A missing board is a 404 with an empty body, the same response a
 * {@code WebApplicationException} with status 404 produced.
 */
@Provider
public class BoardNotFoundExceptionMapper implements ExceptionMapper<BoardNotFoundException> {

    @Override
    public Response toResponse(BoardNotFoundException exception) {
        return Response.status(404).build();
    }
}
