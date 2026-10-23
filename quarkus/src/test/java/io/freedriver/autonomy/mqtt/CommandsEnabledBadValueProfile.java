package io.freedriver.autonomy.mqtt;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

/** Commands flag is not true. MQTT publish stays at its default, off. */
public class CommandsEnabledBadValueProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("autonomy.mqtt.commands-enabled", "banana");
    }
}
