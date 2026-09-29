package io.freedriver.autonomy.mqtt;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.Level;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(MalformedMqttTypedSettingsProfile.class)
class MalformedMqttTypedSettingsStartupTest {

    @Test
    void malformedPortAndKeepaliveLeaveTheAppUpAndMqttOff() {
        given()
                .when().get("/rest/event")
                .then()
                .statusCode(200);

        List<ExtLogRecord> errors = MqttStartupLogCapture.records.stream()
                .filter(record -> record.getLevel().equals(Level.ERROR))
                .toList();
        assertEquals(1, errors.size());
        String message = errors.get(0).getFormattedMessage();
        assertEquals(
                "MQTT off; invalid configuration keys=autonomy.mqtt.keepalive,autonomy.mqtt.port",
                message);
        for (ExtLogRecord record : MqttStartupLogCapture.records) {
            String line = record.getFormattedMessage();
            assertFalse(line.contains(MalformedMqttTypedSettingsProfile.SECRET));
            assertFalse(line.contains(MalformedMqttTypedSettingsProfile.PORT));
            assertFalse(line.contains(MalformedMqttTypedSettingsProfile.KEEPALIVE));
            assertFalse(line.contains("connecting host="));
            assertFalse(line.contains("connected host="));
            assertFalse(line.startsWith("MQTT settings"));
        }
    }
}
