package io.freedriver.autonomy.mqtt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class AutonomyMqttSettingsTest {

    @Test
    void clientIdDefaultsToAutonomyPlusTheInstanceId() {
        UUID instanceId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        AutonomyMqttSettings settings = new AutonomyMqttSettings(config(instanceId.toString().toUpperCase(), Optional.empty()));
        assertEquals(instanceId, settings.instanceId());
        assertEquals("autonomy-" + instanceId, settings.clientId());
        assertTrue(settings.caFile().isEmpty());
    }

    @Test
    void clientIdOverrideIsKept() {
        AutonomyMqttSettings settings = new AutonomyMqttSettings(
                config("550e8400-e29b-41d4-a716-446655440000", Optional.of("home-1")));
        assertEquals("home-1", settings.clientId());
    }

    private static AutonomyMqttConfig config(String instanceId, Optional<String> clientId) {
        return new AutonomyMqttConfig() {
            @Override
            public boolean enabled() {
                return true;
            }

            @Override
            public String host() {
                return "mqtt.freedriver.io";
            }

            @Override
            public int port() {
                return 8883;
            }

            @Override
            public Optional<String> username() {
                return Optional.of("autonomy");
            }

            @Override
            public Optional<String> passwordFile() {
                return Optional.of("/run/secrets/autonomy.pass");
            }

            @Override
            public Optional<String> caFile() {
                return Optional.empty();
            }

            @Override
            public Optional<String> clientId() {
                return clientId;
            }

            @Override
            public Optional<String> instanceId() {
                return Optional.of(instanceId);
            }

            @Override
            public Optional<String> instanceName() {
                return Optional.of("Cabin");
            }

            @Override
            public Duration publishInterval() {
                return Duration.ofSeconds(10);
            }

            @Override
            public Duration keepalive() {
                return Duration.ofSeconds(60);
            }

            @Override
            public Duration connectTimeout() {
                return Duration.ofSeconds(10);
            }
        };
    }
}
