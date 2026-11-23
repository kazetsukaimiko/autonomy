package io.freedriver.autonomy.serial;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    private static final String REQUEST_JSON = """
            {"uuid":null,"requestId":"11111111-1111-1111-1111-111111111111","mode":{"13":false,"14":true},"read":null,"write":{"digital":{"7":false,"8":true},"empty":false},"boardInfo":null,"turn_off":[],"turn_on":[]}\
            """;

    private static final String RESPONSE_JSON = """
            {"version":[1,0,0],"uuid":null,"requestId":"11111111-1111-1111-1111-111111111111","boardInfo":null,"info":[],"error":[],"debug":[],"digital":{},"analog":[],"created":"2026-11-22T00:00:00Z"}\
            """;

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

        ObjectMapper mapper = SerialGatewayJson.mapper();
        String json = mapper.writeValueAsString(request);
        // jsonlink copies maps with Map.copyOf, whose iteration order is unspecified, so the locked
        // string is compared as a tree. The raw text still has to carry both states and the empty flag.
        assertEquals(mapper.readTree(REQUEST_JSON), mapper.readTree(json));
        assertTrue(json.contains("\"13\":false"));
        assertTrue(json.contains("\"14\":true"));
        assertTrue(json.contains("\"7\":false"));
        assertTrue(json.contains("\"8\":true"));
        assertTrue(json.contains("\"empty\":false"));
        assertEquals(request, mapper.readValue(json, Request.class));
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

        ObjectMapper mapper = SerialGatewayJson.mapper();
        String json = mapper.writeValueAsString(response);
        assertEquals(RESPONSE_JSON, json);
        assertEquals(response, mapper.readValue(json, Response.class));
    }

    @Test
    public void errorBodyIsTheMessageOnly() throws Exception {
        assertEquals("X-Autonomy-Serial-Write", SerialGatewayHeaders.WRITE_TOKEN);
        String json = SerialGatewayJson.mapper().writeValueAsString(new SerialGatewayError("board not connected"));
        assertEquals("{\"error\":\"board not connected\"}", json);
    }
}
