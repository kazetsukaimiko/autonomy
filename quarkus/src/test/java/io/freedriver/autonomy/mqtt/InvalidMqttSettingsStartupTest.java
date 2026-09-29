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
@TestProfile(InvalidMqttSettingsProfile.class)
class InvalidMqttSettingsStartupTest {

    @Test
    void invalidInstanceIdLeavesTheAppUpAndMqttOff() {
        given()
                .when().get("/rest/event")
                .then()
                .statusCode(200);

        List<ExtLogRecord> errors = MqttStartupLogCapture.records.stream()
                .filter(record -> record.getLevel().equals(Level.ERROR))
                .filter(record -> record.getFormattedMessage().contains("MQTT off; invalid configuration keys="))
                .toList();
        assertEquals(1, errors.size());
        String message = errors.get(0).getFormattedMessage();
        assertTrue(message.contains("autonomy.mqtt.instanceId"));
        assertFalse(message.contains(InvalidMqttSettingsProfile.SECRET));
        assertFalse(message.contains(InvalidMqttSettingsProfile.INSTANCE_ID));
    }
}
