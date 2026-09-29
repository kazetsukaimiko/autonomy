package io.freedriver.autonomy.mqtt;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Broker and instance settings the service supplies. The password is not a
 * setting; the connector reads {@link #passwordFile()} itself.
 */
public interface MqttSettings {

    boolean enabled();

    String host();

    int port();

    String username();

    Path passwordFile();

    Optional<Path> caFile();

    String clientId();

    UUID instanceId();

    String instanceName();

    Duration publishInterval();

    Duration keepalive();

    Duration connectTimeout();
}
