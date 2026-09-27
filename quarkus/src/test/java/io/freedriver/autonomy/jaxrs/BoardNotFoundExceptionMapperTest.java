package io.freedriver.autonomy.jaxrs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.freedriver.autonomy.service.BoardNotFoundException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

class BoardNotFoundExceptionMapperTest {

    @Test
    void missingBoardIsAnEmpty404() {
        Response response = new BoardNotFoundExceptionMapper()
                .toResponse(new BoardNotFoundException("Board not found, present devices: /dev/ttyACM0"));

        assertEquals(404, response.getStatus());
        assertNull(response.getEntity());
    }
}
