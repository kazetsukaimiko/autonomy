package io.freedriver.autonomy.mqtt;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

/**
 * MQTT on, with an instance id the publisher rejects. The username is a
 * stand-in secret that the startup ERROR must leave out.
 */
public class InvalidMqttSettingsProfile implements QuarkusTestProfile {

    static final String SECRET = "xK9mQ2vL7p";
    static final String INSTANCE_ID = "not-a-uuid";

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "autonomy.mqtt.enabled", "true",
                "autonomy.mqtt.instance-id", INSTANCE_ID,
                "autonomy.mqtt.username", SECRET);
    }
}
