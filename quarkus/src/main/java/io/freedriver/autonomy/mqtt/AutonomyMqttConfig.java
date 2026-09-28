package io.freedriver.autonomy.mqtt;

import java.time.Duration;
import java.util.Optional;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * {@code autonomy.mqtt.*}. Constraints apply when {@link #enabled()} is true.
 */
@ConfigMapping(prefix = "autonomy.mqtt")
@MqttConfigValid
public interface AutonomyMqttConfig {

    @WithDefault("false")
    boolean enabled();

    @WithDefault("mqtt.freedriver.io")
    String host();

    @WithDefault("8883")
    int port();

    Optional<String> username();

    Optional<String> passwordFile();

    Optional<String> caFile();

    Optional<String> clientId();

    Optional<String> instanceId();

    Optional<String> instanceName();

    @WithDefault("10s")
    Duration publishInterval();

    @WithDefault("60s")
    Duration keepalive();

    @WithDefault("10s")
    Duration connectTimeout();
}
