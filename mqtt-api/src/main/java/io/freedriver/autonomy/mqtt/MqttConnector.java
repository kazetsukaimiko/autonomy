package io.freedriver.autonomy.mqtt;

/**
 * Broker connection. {@link #start} retries until {@link #close()}. Command
 * subscribe is separate and stays off unless {@link MqttSettings#commandsEnabled()}
 * is true.
 */
public interface MqttConnector extends AutoCloseable {

    void start(MqttSettings settings, ApplianceStateSource states);

    /**
     * Same loop as {@link #start(MqttSettings, ApplianceStateSource)}. When
     * command handling is on, parsed commands are applied through {@code commands}.
     * A null sink does not subscribe.
     */
    void start(MqttSettings settings, ApplianceStateSource states, ApplianceCommandSink commands);

    /**
     * Publish to an exact topic (no {@code +} or {@code #}). Throws if this
     * connector is not connected. Appliance state publishing stays on the start
     * loop. Metrics uses this with a queueing emitter so a down broker does not
     * throw into GPIO handlers.
     */
    void publishExact(String topic, byte[] payload, int qos, boolean retain);

    @Override
    void close();
}
