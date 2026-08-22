package io.freedriver.autonomy.mqtt.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.Test;

class ApplianceSchemasTest {

    @Test
    void topicA_happyPath_parseAndSerialize() {
        ApplianceStateMessage message = ApplianceStateMessage.parse("""
                {
                  "schemaVersion": 1,
                  "appliedCommandId": "550e8400-e29b-41d4-a716-446655440000",
                  "appliances": [
                    {"id": "living-room-lamp", "name": "Living room lamp", "on": true}
                  ]
                }
                """);
        assertEquals(1, message.schemaVersion());
        assertEquals("550e8400-e29b-41d4-a716-446655440000", message.appliedCommandId());
        assertEquals(1, message.appliances().size());
        assertEquals("living-room-lamp", message.appliances().get(0).id());
        assertEquals("Living room lamp", message.appliances().get(0).name());
        assertTrue(message.appliances().get(0).on());

        ApplianceStateMessage roundTrip = ApplianceStateMessage.parse(message.toJson());
        assertEquals(message, roundTrip);
        assertEquals(ApplianceSchemas.STATE_TOPIC, "freedriver/v1/home/appliances");
        assertEquals(1, ApplianceSchemas.QOS);
        assertFalse(ApplianceSchemas.RETAIN);
    }

    @Test
    void topicB_happyPath_parseAndSerialize() {
        ApplianceCommandMessage command = ApplianceCommandMessage.parse("""
                {
                  "schemaVersion": 1,
                  "commandId": "550e8400-e29b-41d4-a716-446655440000",
                  "applianceId": "living-room-lamp",
                  "on": false
                }
                """);
        assertEquals(1, command.schemaVersion());
        assertEquals("550e8400-e29b-41d4-a716-446655440000", command.commandId());
        assertEquals("living-room-lamp", command.applianceId());
        assertFalse(command.on());

        ApplianceCommandMessage constructed =
                new ApplianceCommandMessage(1, "550e8400-e29b-41d4-a716-446655440000", "living-room-lamp", false);
        assertEquals(constructed, ApplianceCommandMessage.parse(constructed.toJson()));
        assertEquals(ApplianceSchemas.COMMAND_TOPIC, "freedriver/v1/home/commands");
    }

    @Test
    void topicA_rejectsExtraFields() {
        assertThrows(IllegalArgumentException.class, () -> ApplianceStateMessage.parse("""
                {"schemaVersion":1,"appliedCommandId":null,"appliances":[],"nope":true}
                """));
    }

    @Test
    void topicB_rejectsExtraFields() {
        assertThrows(IllegalArgumentException.class, () -> ApplianceCommandMessage.parse("""
                {"schemaVersion":1,"commandId":"cmd-1","applianceId":"living-room-lamp","on":false,"retain":true}
                """));
    }

    @Test
    void topicA_rejectsWrongSchemaVersion() {
        assertThrows(IllegalArgumentException.class, () -> ApplianceStateMessage.parse("""
                {"schemaVersion":2,"appliedCommandId":null,"appliances":[]}
                """));
    }

    @Test
    void topicA_rejectsMissingSchemaVersion() {
        assertThrows(IllegalArgumentException.class, () -> ApplianceStateMessage.parse("""
                {"appliedCommandId":null,"appliances":[]}
                """));
    }

    @Test
    void topicB_rejectsWrongSchemaVersion() {
        assertThrows(IllegalArgumentException.class, () -> ApplianceCommandMessage.parse("""
                {"schemaVersion":2,"commandId":"cmd-1","applianceId":"living-room-lamp","on":false}
                """));
    }

    @Test
    void topicB_rejectsMissingSchemaVersion() {
        assertThrows(IllegalArgumentException.class, () -> ApplianceCommandMessage.parse("""
                {"commandId":"cmd-1","applianceId":"living-room-lamp","on":false}
                """));
    }

    @Test
    void topicA_rejectsInvalidApplianceId() {
        assertThrows(IllegalArgumentException.class, () -> ApplianceStateMessage.parse("""
                {"schemaVersion":1,"appliedCommandId":null,"appliances":[{"id":"Living_Room","name":"Lamp","on":true}]}
                """));
    }

    @Test
    void topicB_rejectsInvalidApplianceId() {
        assertThrows(IllegalArgumentException.class, () -> ApplianceCommandMessage.parse("""
                {"schemaVersion":1,"commandId":"cmd-1","applianceId":"Living_Room","on":false}
                """));
    }

    @Test
    void topicA_allowsNullAppliedCommandId() {
        ApplianceStateMessage message = ApplianceStateMessage.parse("""
                {
                  "schemaVersion": 1,
                  "appliedCommandId": null,
                  "appliances": [
                    {"id": "living-room-lamp", "name": "Living room lamp", "on": true}
                  ]
                }
                """);
        assertNull(message.appliedCommandId());
        assertEquals(List.of(new Appliance("living-room-lamp", "Living room lamp", true)), message.appliances());
    }

    @Test
    void schemasExistOnClasspath() throws Exception {
        try (InputStream appliances = ApplianceSchemas.class.getResourceAsStream(ApplianceSchemas.APPLIANCES_SCHEMA);
                InputStream commands = ApplianceSchemas.class.getResourceAsStream(ApplianceSchemas.COMMANDS_SCHEMA)) {
            assertNotNull(appliances);
            assertNotNull(commands);
        }
    }
}
