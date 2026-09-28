package io.freedriver.autonomy.mqtt;

import java.time.Duration;
import java.util.Optional;

/**
 * Config values {@link MqttStatePublisher} validates. Kept off the
 * {@link AutonomyMqttConfig} mapping so a bad value leaves the process running.
 */
@MqttConfigValid
public record MqttStartupConfig(AutonomyMqttConfig config) {

    public boolean enabled() {
        return config.enabled();
    }

    public String host() {
        return config.host();
    }

    public int port() {
        return config.port();
    }

    public Optional<String> username() {
        return config.username();
    }

    public Optional<String> passwordFile() {
        return config.passwordFile();
    }

    public Optional<String> caFile() {
        return config.caFile();
    }

    public Optional<String> clientId() {
        return config.clientId();
    }

    public Optional<String> instanceId() {
        return config.instanceId();
    }

    public Optional<String> instanceName() {
        return config.instanceName();
    }

    public Duration publishInterval() {
        return config.publishInterval();
    }

    public Duration keepalive() {
        return config.keepalive();
    }

    public Duration connectTimeout() {
        return config.connectTimeout();
    }
}
