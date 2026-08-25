package io.freedriver.autonomy.mqtt.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ApplianceSchemasTest {

    private static final String INSTANCE_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final UUID INSTANCE_UUID = UUID.fromString(INSTANCE_ID);
    private static final UUID V1_UUID = UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8");

    @Test
    void topicA_happyPath_parseAndSerialize() {
        ApplianceStateMessage message = ApplianceStateMessage.parse("""
                {
                  "instanceId": "%s",
                  "instanceName": "Cabin",
                  "appliedCommandId": "550e8400-e29b-41d4-a716-446655440000",
                  "appliances": [
                    {"applianceName": "Living_room_lamp", "on": true}
                  ]
                }
                """.formatted(INSTANCE_ID));
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
    }

    @Test
    void topicB_happyPath_parseAndSerialize() {
        ApplianceCommandMessage command = ApplianceCommandMessage.parse("""
                {
                  "instanceId": "%s",
                  "commandId": "550e8400-e29b-41d4-a716-446655440000",
                  "applianceName": "Living_room_lamp",
                  "on": false
                }
                """.formatted(INSTANCE_ID));
        assertEquals(INSTANCE_UUID, command.instanceId());
        assertEquals("550e8400-e29b-41d4-a716-446655440000", command.commandId());
        assertEquals("Living_room_lamp", command.applianceName());
        assertFalse(command.on());

        ApplianceCommandMessage constructed = new ApplianceCommandMessage(
                INSTANCE_UUID, "550e8400-e29b-41d4-a716-446655440000", "Living_room_lamp", false);
        assertEquals(constructed, ApplianceCommandMessage.parse(constructed.toJson()));
        assertEquals(
                "freedriver/v1/" + INSTANCE_ID + "/commands",
                ApplianceSchemas.commandsTopic(INSTANCE_UUID));
        assertEquals("freedriver/v1/{instanceId}/commands", ApplianceSchemas.COMMANDS_TOPIC_TEMPLATE);
    }

    @Test
    void records_haveToBuilder() {
        Appliance appliance = Appliance.builder().applianceName("Living_room_lamp").on(true).build();
        assertEquals("Living_room_lamp", appliance.applianceName());
        assertEquals("Porch", appliance.toBuilder().applianceName("Porch").build().applianceName());

        ApplianceStateMessage state = ApplianceStateMessage.builder()
                .instanceId(INSTANCE_UUID)
                .instanceName("Cabin")
                .appliances(List.of(appliance))
                .build();
        assertEquals("Cabin", state.toBuilder().build().instanceName());

        ApplianceCommandMessage command = ApplianceCommandMessage.builder()
                .instanceId(INSTANCE_UUID)
                .commandId("cmd-1")
                .applianceName("Living_room_lamp")
                .on(false)
                .build();
        assertFalse(command.toBuilder().build().on());
    }

    @Test
    void topicA_rejectsExtraFields() {
        assertThrows(IllegalArgumentException.class, () -> ApplianceStateMessage.parse("""
                {"instanceId":"%s","instanceName":"Cabin","appliedCommandId":null,"appliances":[],"nope":true}
                """.formatted(INSTANCE_ID)));
    }

    @Test
    void topicB_rejectsExtraFields() {
        assertThrows(IllegalArgumentException.class, () -> ApplianceCommandMessage.parse("""
                {"instanceId":"%s","commandId":"cmd-1","applianceName":"Living_room_lamp","on":false,"retain":true}
                """.formatted(INSTANCE_ID)));
    }

    @Test
    void topicA_rejectsBoardFields() {
        assertThrows(IllegalArgumentException.class, () -> ApplianceStateMessage.parse("""
                {"instanceId":"%s","instanceName":"Cabin","appliedCommandId":null,"appliances":[],"boardId":"b1"}
                """.formatted(INSTANCE_ID)));
    }

    @Test
    void topicA_rejectsNameInsteadOfApplianceName() {
        assertThrows(IllegalArgumentException.class, () -> ApplianceStateMessage.parse("""
                {"instanceId":"%s","instanceName":"Cabin","appliedCommandId":null,"appliances":[{"name":"Living_room_lamp","on":true}]}
                """.formatted(INSTANCE_ID)));
    }

    @Test
    void topicB_rejectsNameInsteadOfApplianceName() {
        assertThrows(IllegalArgumentException.class, () -> ApplianceCommandMessage.parse("""
                {"instanceId":"%s","commandId":"cmd-1","name":"Living_room_lamp","on":false}
                """.formatted(INSTANCE_ID)));
    }

    @Test
    void topicA_rejectsMissingInstanceId() {
        assertThrows(IllegalArgumentException.class, () -> ApplianceStateMessage.parse("""
                {"instanceName":"Cabin","appliedCommandId":null,"appliances":[]}
                """));
    }

    @Test
    void topicB_rejectsMissingInstanceId() {
        assertThrows(IllegalArgumentException.class, () -> ApplianceCommandMessage.parse("""
                {"commandId":"cmd-1","applianceName":"Living_room_lamp","on":false}
                """));
    }

    @Test
    void topicA_rejectsNonUuidInstanceId() {
        assertThrows(IllegalArgumentException.class, () -> ApplianceStateMessage.parse("""
                {"instanceId":"cabin-1","instanceName":"Cabin","appliedCommandId":null,"appliances":[]}
                """));
    }

    @Test
    void topicA_rejectsUuidV1InstanceId() {
        assertThrows(IllegalArgumentException.class, () -> ApplianceStateMessage.parse("""
                {"instanceId":"6ba7b810-9dad-11d1-80b4-00c04fd430c8","instanceName":"Cabin","appliedCommandId":null,"appliances":[]}
                """));
    }

    @Test
    void topicHelpers_rejectNonUuidV4() {
        assertThrows(IllegalArgumentException.class, () -> ApplianceSchemas.appliancesTopic(V1_UUID));
        assertThrows(IllegalArgumentException.class, () -> ApplianceSchemas.commandsTopic(V1_UUID));
        assertEquals("freedriver/v1/" + INSTANCE_ID + "/appliances", ApplianceSchemas.appliancesTopic(INSTANCE_UUID));
        assertEquals("freedriver/v1/" + INSTANCE_ID + "/commands", ApplianceSchemas.commandsTopic(INSTANCE_UUID));
    }

    @Test
    void topicA_rejectsBlankApplianceName() {
        assertThrows(IllegalArgumentException.class, () -> ApplianceStateMessage.parse("""
                {"instanceId":"%s","instanceName":"Cabin","appliedCommandId":null,"appliances":[{"applianceName":"   ","on":true}]}
                """.formatted(INSTANCE_ID)));
    }

    @Test
    void topicB_rejectsBlankApplianceName() {
        assertThrows(IllegalArgumentException.class, () -> ApplianceCommandMessage.parse("""
                {"instanceId":"%s","commandId":"cmd-1","applianceName":"","on":false}
                """.formatted(INSTANCE_ID)));
    }

    @Test
    void topicA_rejectsApplianceNameLongerThan64() {
        String tooLong = "n".repeat(65);
        assertThrows(IllegalArgumentException.class, () -> ApplianceStateMessage.parse("""
                {"instanceId":"%s","instanceName":"Cabin","appliedCommandId":null,"appliances":[{"applianceName":"%s","on":true}]}
                """.formatted(INSTANCE_ID, tooLong)));
    }

    @Test
    void topicA_rejectsBlankInstanceName() {
        assertThrows(IllegalArgumentException.class, () -> ApplianceStateMessage.parse("""
                {"instanceId":"%s","instanceName":"  ","appliedCommandId":null,"appliances":[]}
                """.formatted(INSTANCE_ID)));
    }

    @Test
    void topicA_allowsNullAppliedCommandId() {
        ApplianceStateMessage message = ApplianceStateMessage.parse("""
                {
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
    void topicA_rejectsMissingApplianceName() {
        assertThrows(IllegalArgumentException.class, () -> ApplianceStateMessage.parse("""
                {"instanceId":"%s","instanceName":"Cabin","appliedCommandId":null,"appliances":[{"on":true}]}
                """.formatted(INSTANCE_ID)));
    }

    @Test
    void topicB_rejectsMissingApplianceName() {
        assertThrows(IllegalArgumentException.class, () -> ApplianceCommandMessage.parse("""
                {"instanceId":"%s","commandId":"cmd-1","on":false}
                """));
    }

    @Test
    void topicB_rejectsMissingCommandId() {
        assertThrows(IllegalArgumentException.class, () -> ApplianceCommandMessage.parse("""
                {"instanceId":"%s","applianceName":"Living_room_lamp","on":false}
                """));
    }

    @Test
    void topicA_rejectsMissingAppliances() {
        assertThrows(IllegalArgumentException.class, () -> ApplianceStateMessage.parse("""
                {"instanceId":"%s","instanceName":"Cabin","appliedCommandId":null}
                """.formatted(INSTANCE_ID)));
    }

    @Test
    void topicB_rejectsMissingOn() {
        assertThrows(IllegalArgumentException.class, () -> ApplianceCommandMessage.parse("""
                {"instanceId":"%s","commandId":"cmd-1","applianceName":"Living_room_lamp"}
                """.formatted(INSTANCE_ID)));
    }

    @Test
    void topicA_instanceNameIsNotInTopic() {
        String topic = ApplianceSchemas.appliancesTopic(INSTANCE_UUID);
        assertFalse(topic.contains("Cabin"));
        assertTrue(topic.contains(INSTANCE_ID));
        assertEquals("freedriver/v1/" + INSTANCE_ID + "/appliances", topic);
    }

    @Test
    void topicA_rejectsSchemaVersionField() {
        assertThrows(IllegalArgumentException.class, () -> ApplianceStateMessage.parse("""
                {"schemaVersion":2,"instanceId":"%s","instanceName":"Cabin","appliedCommandId":null,"appliances":[]}
                """.formatted(INSTANCE_ID)));
    }
}
