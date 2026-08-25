package io.freedriver.autonomy.mqtt.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ApplianceSchemasTest {

    private static final String INSTANCE_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final UUID INSTANCE_UUID = UUID.fromString(INSTANCE_ID);

    @Test
    void topicA_happyPath_parseAndSerialize() {
        ApplianceStateMessage message = ApplianceStateMessage.parse("""
                {
                  "schemaVersion": 2,
                  "instanceId": "%s",
                  "instanceName": "Cabin",
                  "appliedCommandId": "550e8400-e29b-41d4-a716-446655440000",
                  "appliances": [
                    {"applianceName": "Living_room_lamp", "on": true}
                  ]
                }
                """.formatted(INSTANCE_ID));
        assertEquals(2, message.schemaVersion());
        assertEquals(INSTANCE_UUID, message.instanceId());
        assertEquals("Cabin", message.instanceName());
        assertEquals("550e8400-e29b-41d4-a716-446655440000", message.appliedCommandId());
        assertEquals(1, message.appliances().size());
        assertEquals("Living_room_lamp", message.appliances().get(0).applianceName());
        assertTrue(message.appliances().get(0).on());

        ApplianceStateMessage roundTrip = ApplianceStateMessage.parse(message.toJson());
        assertEquals(message, roundTrip);
        assertEquals(
                "freedriver/v1/" + INSTANCE_ID + "/appliances",
                ApplianceSchemas.appliancesTopic(INSTANCE_UUID));
        assertEquals("freedriver/v1/{instanceId}/appliances", ApplianceSchemas.APPLIANCES_TOPIC_TEMPLATE);
        assertEquals(1, ApplianceSchemas.QOS);
        assertFalse(ApplianceSchemas.RETAIN);
        assertEquals(2, ApplianceSchemas.SCHEMA_VERSION);
    }

    @Test
    void topicB_happyPath_parseAndSerialize() {
        ApplianceCommandMessage command = ApplianceCommandMessage.parse("""
                {
                  "schemaVersion": 2,
                  "instanceId": "%s",
                  "commandId": "550e8400-e29b-41d4-a716-446655440000",
                  "applianceName": "Living_room_lamp",
                  "on": false
                }
                """.formatted(INSTANCE_ID));
        assertEquals(2, command.schemaVersion());
        assertEquals(INSTANCE_UUID, command.instanceId());
        assertEquals("550e8400-e29b-41d4-a716-446655440000", command.commandId());
        assertEquals("Living_room_lamp", command.applianceName());
        assertFalse(command.on());

        ApplianceCommandMessage constructed = new ApplianceCommandMessage(
                2, INSTANCE_UUID, "550e8400-e29b-41d4-a716-446655440000", "Living_room_lamp", false);
        assertEquals(constructed, ApplianceCommandMessage.parse(constructed.toJson()));
        assertEquals(
                "freedriver/v1/" + INSTANCE_ID + "/commands",
                ApplianceSchemas.commandsTopic(INSTANCE_UUID));
        assertEquals("freedriver/v1/{instanceId}/commands", ApplianceSchemas.COMMANDS_TOPIC_TEMPLATE);
    }

    @Test
    void topicA_rejectsExtraFields() {
        assertThrows(IllegalArgumentException.class, () -> ApplianceStateMessage.parse("""
                {"schemaVersion":2,"instanceId":"%s","instanceName":"Cabin","appliedCommandId":null,"appliances":[],"nope":true}
                """.formatted(INSTANCE_ID)));
    }

    @Test
    void topicB_rejectsExtraFields() {
        assertThrows(IllegalArgumentException.class, () -> ApplianceCommandMessage.parse("""
                {"schemaVersion":2,"instanceId":"%s","commandId":"cmd-1","applianceName":"Living_room_lamp","on":false,"retain":true}
                """.formatted(INSTANCE_ID)));
    }

    @Test
    void topicA_rejectsBoardFields() {
        assertThrows(IllegalArgumentException.class, () -> ApplianceStateMessage.parse("""
                {"schemaVersion":2,"instanceId":"%s","instanceName":"Cabin","appliedCommandId":null,"appliances":[],"boardId":"b1"}
                """.formatted(INSTANCE_ID)));
    }

    @Test
    void topicA_rejectsNameInsteadOfApplianceName() {
        assertThrows(IllegalArgumentException.class, () -> ApplianceStateMessage.parse("""
                {"schemaVersion":2,"instanceId":"%s","instanceName":"Cabin","appliedCommandId":null,"appliances":[{"name":"Living_room_lamp","on":true}]}
                """.formatted(INSTANCE_ID)));
    }

    @Test
    void topicB_rejectsNameInsteadOfApplianceName() {
        assertThrows(IllegalArgumentException.class, () -> ApplianceCommandMessage.parse("""
                {"schemaVersion":2,"instanceId":"%s","commandId":"cmd-1","name":"Living_room_lamp","on":false}
                """.formatted(INSTANCE_ID)));
    }

    @Test
    void topicA_rejectsMissingInstanceId() {
        assertThrows(IllegalArgumentException.class, () -> ApplianceStateMessage.parse("""
                {"schemaVersion":2,"instanceName":"Cabin","appliedCommandId":null,"appliances":[]}
                """));
    }

    @Test
    void topicB_rejectsMissingInstanceId() {
        assertThrows(IllegalArgumentException.class, () -> ApplianceCommandMessage.parse("""
                {"schemaVersion":2,"commandId":"cmd-1","applianceName":"Living_room_lamp","on":false}
                """));
    }

    @Test
    void topicA_rejectsNonUuidInstanceId() {
        assertThrows(IllegalArgumentException.class, () -> ApplianceStateMessage.parse("""
                {"schemaVersion":2,"instanceId":"cabin-1","instanceName":"Cabin","appliedCommandId":null,"appliances":[]}
                """));
    }

    @Test
    void topicA_rejectsUuidV1InstanceId() {
        assertThrows(IllegalArgumentException.class, () -> ApplianceStateMessage.parse("""
                {"schemaVersion":2,"instanceId":"6ba7b810-9dad-11d1-80b4-00c04fd430c8","instanceName":"Cabin","appliedCommandId":null,"appliances":[]}
                """));
    }

    @Test
    void topicHelpers_rejectMqttWildcardsInId() {
        assertThrows(IllegalArgumentException.class, () -> ApplianceSchemas.requireSafeTopicSegment("foo/bar"));
        assertThrows(IllegalArgumentException.class, () -> ApplianceSchemas.requireSafeTopicSegment("foo+bar"));
        assertThrows(IllegalArgumentException.class, () -> ApplianceSchemas.requireSafeTopicSegment("foo#bar"));
        assertThrows(IllegalArgumentException.class, () -> ApplianceSchemas.requireSafeTopicSegment("not-a-uuid"));
        assertThrows(
                IllegalArgumentException.class,
                () -> ApplianceSchemas.requireInstanceId(
                        UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8")));
        assertEquals(INSTANCE_ID, ApplianceSchemas.requireInstanceId(INSTANCE_UUID));
        assertEquals(INSTANCE_ID, ApplianceSchemas.requireSafeTopicSegment(INSTANCE_ID));
    }

    @Test
    void topicA_rejectsWrongSchemaVersion() {
        assertThrows(IllegalArgumentException.class, () -> ApplianceStateMessage.parse("""
                {"schemaVersion":1,"instanceId":"%s","instanceName":"Cabin","appliedCommandId":null,"appliances":[]}
                """.formatted(INSTANCE_ID)));
    }

    @Test
    void topicA_rejectsMissingSchemaVersion() {
        assertThrows(IllegalArgumentException.class, () -> ApplianceStateMessage.parse("""
                {"instanceId":"%s","instanceName":"Cabin","appliedCommandId":null,"appliances":[]}
                """.formatted(INSTANCE_ID)));
    }

    @Test
    void topicB_rejectsWrongSchemaVersion() {
        assertThrows(IllegalArgumentException.class, () -> ApplianceCommandMessage.parse("""
                {"schemaVersion":1,"instanceId":"%s","commandId":"cmd-1","applianceName":"Living_room_lamp","on":false}
                """.formatted(INSTANCE_ID)));
    }

    @Test
    void topicB_rejectsMissingSchemaVersion() {
        assertThrows(IllegalArgumentException.class, () -> ApplianceCommandMessage.parse("""
                {"instanceId":"%s","commandId":"cmd-1","applianceName":"Living_room_lamp","on":false}
                """.formatted(INSTANCE_ID)));
    }

    @Test
    void topicA_rejectsBlankApplianceName() {
        assertThrows(IllegalArgumentException.class, () -> ApplianceStateMessage.parse("""
                {"schemaVersion":2,"instanceId":"%s","instanceName":"Cabin","appliedCommandId":null,"appliances":[{"applianceName":"   ","on":true}]}
                """.formatted(INSTANCE_ID)));
    }

    @Test
    void topicB_rejectsBlankApplianceName() {
        assertThrows(IllegalArgumentException.class, () -> ApplianceCommandMessage.parse("""
                {"schemaVersion":2,"instanceId":"%s","commandId":"cmd-1","applianceName":"","on":false}
                """.formatted(INSTANCE_ID)));
    }

    @Test
    void topicA_rejectsApplianceNameLongerThan64() {
        String tooLong = "n".repeat(65);
        assertThrows(IllegalArgumentException.class, () -> ApplianceStateMessage.parse("""
                {"schemaVersion":2,"instanceId":"%s","instanceName":"Cabin","appliedCommandId":null,"appliances":[{"applianceName":"%s","on":true}]}
                """.formatted(INSTANCE_ID, tooLong)));
    }

    @Test
    void topicA_rejectsBlankInstanceName() {
        assertThrows(IllegalArgumentException.class, () -> ApplianceStateMessage.parse("""
                {"schemaVersion":2,"instanceId":"%s","instanceName":"  ","appliedCommandId":null,"appliances":[]}
                """.formatted(INSTANCE_ID)));
    }

    @Test
    void topicA_allowsNullAppliedCommandId() {
        ApplianceStateMessage message = ApplianceStateMessage.parse("""
                {
                  "schemaVersion": 2,
                  "instanceId": "%s",
                  "instanceName": "Cabin",
                  "appliedCommandId": null,
                  "appliances": [
                    {"applianceName": "Living_room_lamp", "on": true}
                  ]
                }
                """.formatted(INSTANCE_ID));
        assertNull(message.appliedCommandId());
        assertEquals(List.of(new Appliance("Living_room_lamp", true)), message.appliances());
    }

    @Test
    void schemasExistOnClasspath() throws Exception {
        try (InputStream appliances = ApplianceSchemas.class.getResourceAsStream(ApplianceSchemas.APPLIANCES_SCHEMA);
                InputStream commands = ApplianceSchemas.class.getResourceAsStream(ApplianceSchemas.COMMANDS_SCHEMA)) {
            assertNotNull(appliances);
            assertNotNull(commands);
        }
    }

    @Test
    void topicA_rejectsMissingApplianceName() {
        assertThrows(IllegalArgumentException.class, () -> ApplianceStateMessage.parse("""
                {"schemaVersion":2,"instanceId":"%s","instanceName":"Cabin","appliedCommandId":null,"appliances":[{"on":true}]}
                """.formatted(INSTANCE_ID)));
    }

    @Test
    void topicB_rejectsMissingApplianceName() {
        assertThrows(IllegalArgumentException.class, () -> ApplianceCommandMessage.parse("""
                {"schemaVersion":2,"instanceId":"%s","commandId":"cmd-1","on":false}
                """.formatted(INSTANCE_ID)));
    }

    @Test
    void topicB_rejectsMissingCommandId() {
        assertThrows(IllegalArgumentException.class, () -> ApplianceCommandMessage.parse("""
                {"schemaVersion":2,"instanceId":"%s","applianceName":"Living_room_lamp","on":false}
                """.formatted(INSTANCE_ID)));
    }

    @Test
    void topicA_rejectsMissingAppliances() {
        assertThrows(IllegalArgumentException.class, () -> ApplianceStateMessage.parse("""
                {"schemaVersion":2,"instanceId":"%s","instanceName":"Cabin","appliedCommandId":null}
                """.formatted(INSTANCE_ID)));
    }

    @Test
    void topicB_rejectsMissingOn() {
        assertThrows(IllegalArgumentException.class, () -> ApplianceCommandMessage.parse("""
                {"schemaVersion":2,"instanceId":"%s","commandId":"cmd-1","applianceName":"Living_room_lamp"}
                """.formatted(INSTANCE_ID)));
    }

    @Test
    void topicA_instanceNameIsNotInTopic() {
        String topic = ApplianceSchemas.appliancesTopic(INSTANCE_UUID);
        assertFalse(topic.contains("Cabin"));
        assertTrue(topic.contains(INSTANCE_ID));
        assertEquals("freedriver/v1/" + INSTANCE_ID + "/appliances", topic);
    }
}
