package io.freedriver.autonomy.mqtt;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

/**
 * MQTT left at its default ({@code enabled} unset) with a port that is not a number.
 */
public class MqttDisabledBadPortProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("autonomy.mqtt.port", "abc");
    }
}
