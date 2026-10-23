package io.freedriver.autonomy.mqtt;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.Level;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(CommandsEnabledBadValueProfile.class)
class CommandsEnabledBadValueStartupTest {

    @Test
    void aBadCommandsValueLogsOnceAndLeavesTheProcessUp() {
        given()
                .when().get("/rest/event")
                .then()
                .statusCode(200);

        List<ExtLogRecord> lines = MqttStartupLogCapture.records.stream()
                .filter(record -> record.getFormattedMessage().contains("commands not subscribed"))
                .toList();
        assertEquals(1, lines.size());
        String message = lines.get(0).getFormattedMessage();
        assertEquals(Level.INFO, lines.get(0).getLevel());
        assertTrue(message.contains("autonomy.mqtt.commands-enabled is not true"));
        assertFalse(message.contains("banana"));
        assertTrue(MqttStartupLogCapture.records.stream().noneMatch(record ->
                record.getFormattedMessage().contains("commands subscribed")
                        || record.getFormattedMessage().contains("connecting host=")));
    }
}
