package io.freedriver.autonomy.mqtt;

import java.util.Optional;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * {@code autonomy.mqtt.*} as text. Quarkus stores every value as a string so a
 * malformed port, flag, or duration leaves the process running.
 * {@link MqttStatePublisher} parses {@link MqttStartupConfig} after startup.
 */
@ConfigMapping(prefix = "autonomy.mqtt")
public interface AutonomyMqttConfig {

    @WithDefault("false")
    String enabled();

    @WithDefault("mqtt.freedriver.io")
    String host();

    @WithDefault("8883")
    String port();

    Optional<String> username();

    Optional<String> passwordFile();

    Optional<String> caFile();

    Optional<String> clientId();

    Optional<String> instanceId();

    Optional<String> instanceName();

    @WithDefault("10s")
    String publishInterval();

    @WithDefault("60s")
    String keepalive();

    @WithDefault("10s")
    String connectTimeout();
}
