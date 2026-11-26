package io.freedriver.autonomy.serial.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.freedriver.jsonlink.api.v1.BoardRequest;
import io.freedriver.jsonlink.api.v1.BoardResponse;
import io.freedriver.jsonlink.api.v1.DigitalState;
import io.freedriver.jsonlink.api.v1.Identifier;
import io.freedriver.jsonlink.api.v1.Mode;
import io.freedriver.jsonlink.api.v1.WriteRequest;
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
        BoardRequest request = new BoardRequest(
                null,
                REQUEST_ID,
                mode,
                null,
                new WriteRequest(digital),
                null,
                List.of(),
                List.of());

        BoardRequest again = roundTrip(request, BoardRequest.class);
        assertEquals(request, again);
        assertEquals(request.hashCode(), again.hashCode());
    }

    @Test
    public void responseRoundTripKeepsDigitalMap() throws Exception {
        Map<Identifier, Boolean> digital = new LinkedHashMap<>();
        digital.put(Identifier.of(7), Boolean.FALSE);
        digital.put(Identifier.of(8), Boolean.TRUE);
        BoardResponse response = new BoardResponse(
                null,
                REQUEST_ID,
                null,
                List.of(),
                List.of(),
                List.of(),
                digital,
                List.of());

        BoardResponse again = roundTrip(response, BoardResponse.class);
        assertEquals(response, again);
        assertEquals(response.hashCode(), again.hashCode());
    }

    @Test
    public void errorBodyRoundTrips() throws Exception {
        SerialGatewayError error = new SerialGatewayError("board not connected");
        assertEquals(error, roundTrip(error, SerialGatewayError.class));
    }

    private static <T> T roundTrip(T value, Class<T> type) throws Exception {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        return mapper.readValue(mapper.writeValueAsString(value), type);
    }
}
