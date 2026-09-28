package io.freedriver.autonomy.mqtt;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * {@link MqttSettings} backed by {@link AutonomyMqttConfig}. Built only after
 * validation, and only when MQTT is enabled.
 */
public final class AutonomyMqttSettings implements MqttSettings {

    private final AutonomyMqttConfig config;
    private final UUID instanceId;

    public AutonomyMqttSettings(AutonomyMqttConfig config) {
        this.config = config;
        this.instanceId = UUID.fromString(config.instanceId().orElseThrow());
    }

    @Override
    public boolean enabled() {
        return config.enabled();
    }

    @Override
    public String host() {
        return config.host();
    }

    @Override
    public int port() {
        return config.port();
    }

    @Override
    public String username() {
        return config.username().orElseThrow();
    }

    @Override
    public Path passwordFile() {
        return Path.of(config.passwordFile().orElseThrow());
    }

    @Override
    public Optional<Path> caFile() {
        return config.caFile().filter(path -> !path.isBlank()).map(Path::of);
    }

    @Override
    public String clientId() {
        return config.clientId().filter(id -> !id.isBlank()).orElse("autonomy-" + instanceId);
    }

    @Override
    public UUID instanceId() {
        return instanceId;
    }

    @Override
    public String instanceName() {
        return config.instanceName().orElseThrow();
    }

    @Override
    public Duration publishInterval() {
        return config.publishInterval();
    }

    @Override
    public Duration keepalive() {
        return config.keepalive();
    }

    @Override
    public Duration connectTimeout() {
        return config.connectTimeout();
    }
}
