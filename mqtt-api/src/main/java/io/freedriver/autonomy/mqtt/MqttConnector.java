package io.freedriver.autonomy.mqtt;

/**
 * Outbound publish-only connection. {@link #start} retries until {@link #close()}.
 */
public interface MqttConnector extends AutoCloseable {

    void start(MqttSettings settings, ApplianceStateSource states);

    @Override
    void close();
}
