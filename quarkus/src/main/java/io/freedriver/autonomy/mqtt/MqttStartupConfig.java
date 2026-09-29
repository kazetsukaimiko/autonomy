package io.freedriver.autonomy.mqtt;

import java.util.Optional;

/**
 * Text values {@link MqttStatePublisher} validates. Parsing happens here, not
 * while Quarkus loads {@link AutonomyMqttConfig}.
 */
@MqttConfigValid
public record MqttStartupConfig(AutonomyMqttConfig config) {

    public String enabled() {
        return config.enabled();
    }

    public String host() {
        return config.host();
    }

    public String port() {
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

    public String publishInterval() {
        return config.publishInterval();
    }

    public String keepalive() {
        return config.keepalive();
    }

    public String connectTimeout() {
        return config.connectTimeout();
    }
}
