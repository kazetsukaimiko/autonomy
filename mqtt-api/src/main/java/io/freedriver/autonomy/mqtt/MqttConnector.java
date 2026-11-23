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
     * A null sink leaves the commands topic unsubscribed.
     */
    void start(MqttSettings settings, ApplianceStateSource states, ApplianceCommandSink commands);

    /**
     * Same loop as {@link #start(MqttSettings, ApplianceStateSource, ApplianceCommandSink)}.
     * A null {@code lists} leaves the command-lists topic unsubscribed.
     * {@code pending} supplies the scheduled snapshot. The default calls the
     * three-argument start, so a connector overrides this when it subscribes.
     */
    default void start(MqttSettings settings, ApplianceStateSource states, ApplianceCommandSink commands,
            CommandListSink lists, PendingSource pending) {
        start(settings, states, commands);
    }

    /**
     * Listener the house calls after a delayed entry fires. Before {@link #start},
     * the house keeps its own no-op listener and skips the acknowledgement.
     * The default rejects a null id. A connector overrides this to enqueue the id.
     */
    default ScheduledListener scheduledListener() {
        return entryId -> {
            if (entryId == null) {
                throw new IllegalArgumentException("entryId");
            }
        };
    }

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
