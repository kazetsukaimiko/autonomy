package io.freedriver.autonomy.mqtt;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

/**
 * MQTT requested, with a port and keepalive Quarkus would previously reject
 * while converting config. The username is a stand-in secret.
 */
public class MalformedMqttTypedSettingsProfile implements QuarkusTestProfile {

    static final String SECRET = "xK9mQ2vL7p";
    static final String PORT = "abc";
    static final String KEEPALIVE = "soon";

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "autonomy.mqtt.enabled", "true",
                "autonomy.mqtt.port", PORT,
                "autonomy.mqtt.keepalive", KEEPALIVE,
                "autonomy.mqtt.username", SECRET,
                "autonomy.mqtt.password-file", "/home/user/.config/autonomy/mqtt-password",
                "autonomy.mqtt.instance-id", "00000000-0000-4000-8000-000000000000",
                "autonomy.mqtt.instance-name", "example");
    }
}
