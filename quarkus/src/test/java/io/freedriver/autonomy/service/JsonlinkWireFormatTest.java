package io.freedriver.autonomy.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.freedriver.autonomy.jaxrs.ObjectMapperContextResolver;
import io.freedriver.jsonlink.Connector;
import io.freedriver.jsonlink.ResponseAccumulator;
import io.freedriver.jsonlink.api.v1.BoardRequest;
import io.freedriver.jsonlink.api.v1.BoardResponse;
import io.freedriver.jsonlink.api.v1.DigitalState;
import io.freedriver.jsonlink.api.v1.DigitalWrite;
import io.freedriver.jsonlink.api.v1.Identifier;
import io.freedriver.jsonlink.api.v1.Mode;
import io.freedriver.jsonlink.api.v1.ModeSet;
import org.junit.jupiter.api.Test;

/**
 * Wire JSON captured from freedriver 1.0.0 {@code Connector.MAPPER} and the REST mapper
 * before the pin moved. Pin 13 is HIGH and OUTPUT; pin 12 is LOW and INPUT.
 */
class JsonlinkWireFormatTest {

    private static final UUID BOARD = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID REQUEST_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final Identifier HIGH_PIN = Identifier.of(13);
    private static final Identifier LOW_PIN = Identifier.of(12);

    @Test
    void serialCodecMatchesTheCapturedBoardDocument() throws Exception {
        BoardRequest request = request();
        // Same call SerialConnector.makeRequest makes: one compact object, no indent.
        String encoded = Connector.MAPPER.writeValueAsString(request);
        assertCompactSerialLine(encoded);

        JsonNode actual = Connector.MAPPER.readTree(encoded);
        JsonNode expected = Connector.MAPPER.readTree(fixture("/jsonlink/freedriver-1.0.0-digital-write-request.json"));
        assertEquals(expected.get("requestId").asText(), actual.get("requestId").asText());
        assertEquals(REQUEST_ID.toString(), actual.get("requestId").asText());
        assertEquals(expected.get("mode"), actual.get("mode"));
        assertEquals(expected.get("write").get("digital"), actual.get("write").get("digital"));
        assertDecimalPinKeys(actual.get("mode"));
        assertDecimalPinKeys(actual.get("write").get("digital"));
        // jsonlink.ino writePins reads write.digital. write.empty is not a board field.
        assertFalse(actual.get("write").has("empty"));
        if (actual.has("read") && !actual.get("read").isNull()) {
            assertEquals(expected.path("read").path("digital"), actual.get("read").path("digital"));
            assertEquals(expected.path("read").path("analog"), actual.get("read").path("analog"));
        }

        assertEquals(Mode.OUTPUT.getModeValue(), actual.get("mode").get("13").booleanValue());
        assertEquals(Mode.INPUT.getModeValue(), actual.get("mode").get("12").booleanValue());
        assertEquals(DigitalState.HIGH.getValue(), actual.get("write").get("digital").get("13").booleanValue());
        assertEquals(DigitalState.LOW.getValue(), actual.get("write").get("digital").get("12").booleanValue());
        assertEquals(true, Mode.OUTPUT.getModeValue());
        assertEquals(false, Mode.INPUT.getModeValue());
        assertEquals(false, DigitalState.HIGH.getValue());
        assertEquals(true, DigitalState.LOW.getValue());

        BoardRequest decoded = Connector.MAPPER.readValue(encoded, BoardRequest.class);
        assertEquals(Mode.OUTPUT, decoded.mode().get(HIGH_PIN));
        assertEquals(Mode.INPUT, decoded.mode().get(LOW_PIN));
        assertEquals(DigitalState.HIGH, decoded.write().digital().get(HIGH_PIN));
        assertEquals(DigitalState.LOW, decoded.write().digital().get(LOW_PIN));
        assertEquals(13, HIGH_PIN.pin());
        assertEquals(12, LOW_PIN.pin());

        BoardRequest fromFixture = Connector.MAPPER.readValue(
                fixture("/jsonlink/freedriver-1.0.0-digital-write-request.json"), BoardRequest.class);
        assertEquals(decoded.mode(), fromFixture.mode());
        assertEquals(decoded.write().digital(), fromFixture.write().digital());
    }

    @Test
    void responseAccumulatorReadsTheCapturedDigitalMap() throws Exception {
        byte[] line = (fixture("/jsonlink/freedriver-1.0.0-digital-map-response.json") + "\n")
                .getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream frame = new ByteArrayOutputStream();
        frame.write(line);
        ResponseAccumulator accumulator = new ResponseAccumulator();
        assertTrue(accumulator.isComplete(frame));
        BoardResponse response = accumulator.convert(frame);

        assertEquals(REQUEST_ID, response.requestId());
        JsonNode body = Connector.MAPPER.readTree(fixture("/jsonlink/freedriver-1.0.0-digital-map-response.json"));
        assertDecimalPinKeys(body.get("digital"));
        assertEquals(Boolean.FALSE, response.digital().get(HIGH_PIN));
        assertEquals(Boolean.TRUE, response.digital().get(LOW_PIN));
        assertEquals(DigitalState.HIGH.getValue(), response.digital().get(HIGH_PIN));
        assertEquals(DigitalState.LOW.getValue(), response.digital().get(LOW_PIN));
        assertEquals(13, response.digital().keySet().stream().filter(pin -> pin.pin() == 13).findFirst().orElseThrow().pin());
        assertEquals(12, response.digital().keySet().stream().filter(pin -> pin.pin() == 12).findFirst().orElseThrow().pin());
    }

    @Test
    void restMapperMatchesTheCapturedSchemaDocument() throws Exception {
        String encoded = ObjectMapperContextResolver.getMapper().writeValueAsString(request());
        JsonNode actual = ObjectMapperContextResolver.getMapper().readTree(encoded);
        JsonNode expected = ObjectMapperContextResolver.getMapper()
                .readTree(fixture("/jsonlink/freedriver-1.0.0-rest-request.json"));
        ((ObjectNode) expected.get("write")).remove("empty");
        assertEquals(expected, actual);

        String restFixture = fixture("/jsonlink/freedriver-1.0.0-rest-request.json");
        assertTrue(restFixture.contains("\"empty\":false"));
        BoardRequest decoded = ObjectMapperContextResolver.getMapper().readValue(restFixture, BoardRequest.class);
        assertConsumed(decoded);
        assertEquals(REQUEST_ID, decoded.requestId());

        String savedWrite = fixture("/jsonlink/freedriver-1.0.0-digital-write-request.json");
        assertTrue(savedWrite.contains("\"empty\":false"));
        BoardRequest fromSaved = ObjectMapperContextResolver.getMapper().readValue(savedWrite, BoardRequest.class);
        assertConsumed(fromSaved);
        String rewritten = ObjectMapperContextResolver.getMapper().writeValueAsString(fromSaved);
        BoardRequest again = ObjectMapperContextResolver.getMapper().readValue(rewritten, BoardRequest.class);
        assertEquals(fromSaved.requestId(), again.requestId());
        assertEquals(fromSaved.mode(), again.mode());
        assertEquals(fromSaved.write().digital(), again.write().digital());

        BoardResponse savedReply = ObjectMapperContextResolver.getMapper().readValue(
                fixture("/jsonlink/freedriver-1.0.0-digital-map-response.json"), BoardResponse.class);
        assertEquals(REQUEST_ID, savedReply.requestId());
        assertEquals(Boolean.FALSE, savedReply.digital().get(HIGH_PIN));
        assertEquals(Boolean.TRUE, savedReply.digital().get(LOW_PIN));
    }

    private static void assertConsumed(BoardRequest decoded) {
        assertEquals(Mode.OUTPUT, decoded.mode().get(HIGH_PIN));
        assertEquals(Mode.INPUT, decoded.mode().get(LOW_PIN));
        assertEquals(DigitalState.HIGH, decoded.write().digital().get(HIGH_PIN));
        assertEquals(DigitalState.LOW, decoded.write().digital().get(LOW_PIN));
        assertEquals(Mode.OUTPUT, Mode.ofModeValue(true));
        assertEquals(Mode.INPUT, Mode.ofModeValue(false));
        assertEquals(DigitalState.LOW, DigitalState.fromBoolean(true));
        assertEquals(DigitalState.HIGH, DigitalState.fromBoolean(false));
    }

    private static void assertCompactSerialLine(String encoded) {
        assertTrue(encoded.startsWith("{") && encoded.endsWith("}"), encoded);
        assertTrue(encoded.chars().noneMatch(Character::isWhitespace), encoded);
    }

    private static void assertDecimalPinKeys(JsonNode object) {
        assertTrue(object != null && object.isObject());
        object.fieldNames().forEachRemaining(key -> assertTrue(key.matches("[0-9]+"), key));
    }

    private static BoardRequest request() {
        BoardRequest built = BoardRequest.empty()
                .digitalWrite(new DigitalWrite(HIGH_PIN, DigitalState.HIGH))
                .digitalWrite(new DigitalWrite(LOW_PIN, DigitalState.LOW))
                .modeSet(new ModeSet(HIGH_PIN, Mode.OUTPUT))
                .modeSet(new ModeSet(LOW_PIN, Mode.INPUT));
        return new BoardRequest(
                BOARD,
                REQUEST_ID,
                built.mode(),
                built.read(),
                built.write(),
                built.boardInfo(),
                built.turn_off(),
                built.turn_on());
    }

    private static String fixture(String name) throws IOException {
        try (InputStream in = JsonlinkWireFormatTest.class.getResourceAsStream(name)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
        }
    }
}
