package io.freedriver.autonomy.metrics;

import io.freedriver.autonomy.mqtt.AutonomyMqttConfig;
import io.freedriver.autonomy.mqtt.MqttStatePublisher;
import io.freedriver.metrics.InstancedMetricsEmitter;
import io.freedriver.metrics.MetricsEmitter;
import io.freedriver.metrics.MqttMetricsEmitter;
import io.freedriver.metrics.NoopMetricsEmitter;
import io.freedriver.metrics.QueueingMetricsEmitter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

/**
 * Host wiring for the shared emitters: instanced tags plus mqtt, with queueing
 * while the broker is down. MQTT off uses noop so a disabled connector does not
 * queue forever.
 */
@ApplicationScoped
public class AutonomyMetricsProducer {

    static final String UNASSIGNED_INSTANCE = "unassigned";

    @Inject
    AutonomyMqttConfig config;

    @Inject
    MqttStatePublisher mqtt;

    @Produces
    @ApplicationScoped
    MetricsEmitter metricsEmitter() {
        return create(config.instanceId().orElse(null), mqttEnabled(config.enabled()), mqtt::publishExact);
    }

    static MetricsEmitter create(String instanceId, boolean mqttOn, MqttMetricsEmitter.Publisher publisher) {
        String id = instanceId == null || instanceId.isBlank() ? UNASSIGNED_INSTANCE : instanceId.strip();
        MetricsEmitter transport = NoopMetricsEmitter.INSTANCE;
        if (mqttOn) {
            try {
                transport = new QueueingMetricsEmitter(
                        new MqttMetricsEmitter(MqttMetricsEmitter.topic(id), publisher));
            } catch (RuntimeException e) {
                transport = NoopMetricsEmitter.INSTANCE;
            }
        }
        return new InstancedMetricsEmitter(id, transport);
    }

    static boolean mqttEnabled(String raw) {
        return raw != null && raw.trim().equalsIgnoreCase("true");
    }
}
