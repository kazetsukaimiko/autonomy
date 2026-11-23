package io.freedriver.autonomy.serial;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.freedriver.jsonlink.jackson.schema.base.Version;
import io.freedriver.jsonlink.jackson.schema.v1.DigitalState;
import io.freedriver.jsonlink.jackson.schema.v1.Identifier;
import io.freedriver.jsonlink.jackson.schema.v1.Mode;
import io.freedriver.jsonlink.jackson.schema.v1.Request;
import io.freedriver.jsonlink.jackson.schema.v1.Response;
import io.freedriver.jsonlink.jackson.schema.v1.WriteRequest;
import org.junit.jupiter.api.Test;

public class SerialGatewayJsonTest {

    private static final UUID REQUEST_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    public void requestRoundTripKeepsModeAndDigitalWrite() throws Exception {
        Map<Identifier, Mode> mode = new LinkedHashMap<>();
        mode.put(Identifier.of(13), Mode.INPUT);
        mode.put(Identifier.of(14), Mode.OUTPUT);
        Map<Identifier, DigitalState> digital = new LinkedHashMap<>();
        digital.put(Identifier.of(7), DigitalState.HIGH);
        digital.put(Identifier.of(8), DigitalState.LOW);
        Request request = new Request(
                null,
                REQUEST_ID,
                mode,
                null,
                new WriteRequest(digital),
                null,
                List.of(),
                List.of());

        Request again = roundTrip(request, Request.class);
        assertEquals(request, again);
        assertEquals(request.hashCode(), again.hashCode());
    }

    @Test
    public void responseRoundTripKeepsVersionAndInstant() throws Exception {
        Response response = new Response(
                new Version(1, 0, 0),
                null,
                REQUEST_ID,
                null,
                null,
                null,
                null,
                null,
                null,
                Instant.parse("2026-11-22T00:00:00Z"));

        Response again = roundTrip(response, Response.class);
        assertEquals(response, again);
        assertEquals(response.hashCode(), again.hashCode());
    }

    @Test
    public void errorBodyRoundTrips() throws Exception {
        assertEquals("X-Autonomy-Serial-Write", SerialGatewayHeaders.WRITE_TOKEN);
        SerialGatewayError error = new SerialGatewayError("board not connected");
        assertEquals(error, roundTrip(error, SerialGatewayError.class));
    }

    private static <T> T roundTrip(T value, Class<T> type) throws Exception {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        return mapper.readValue(mapper.writeValueAsString(value), type);
    }
}
