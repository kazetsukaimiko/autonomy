package io.freedriver.autonomy.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.freedriver.metrics.InstancedMetricsEmitter;
import io.freedriver.metrics.MetricsJson;
import io.freedriver.metrics.MetricsKind;
import io.freedriver.metrics.MetricsPoint;
import io.freedriver.metrics.MqttMetricsEmitter;
import org.junit.jupiter.api.Test;

class AutonomyMetricsProducerTest {

    private static final String INSTANCE = "550e8400-e29b-41d4-a716-446655440000";

    @Test
    void mqttOffDoesNotPublish() {
        List<String> topics = new ArrayList<>();
        var emitter = AutonomyMetricsProducer.create(INSTANCE, false, (topic, payload, qos, retain) -> topics.add(topic));
        emitter.increment(EventMetricsObserver.KEY, Map.of(EventMetricsObserver.TYPE_TAG, "joystick.event"));
        assertTrue(topics.isEmpty());
        assertEquals(INSTANCE, ((InstancedMetricsEmitter) emitter).instanceId());
    }

    @Test
    void mqttOnPublishesInstancedPointToTheMetricsTopic() {
        List<Published> published = new ArrayList<>();
        var emitter = AutonomyMetricsProducer.create(
                INSTANCE, true, (topic, payload, qos, retain) -> published.add(new Published(topic, payload, qos, retain)));
        emitter.increment(EventMetricsObserver.KEY, Map.of(EventMetricsObserver.TYPE_TAG, "joystick.event"));
        assertEquals(1, published.size());
        Published message = published.get(0);
        assertEquals(MqttMetricsEmitter.topic(INSTANCE), message.topic());
        assertEquals(MqttMetricsEmitter.QOS, message.qos());
        assertEquals(MqttMetricsEmitter.RETAIN, message.retain());
        MetricsPoint point = MetricsJson.read(message.payload());
        assertEquals(EventMetricsObserver.KEY, point.key());
        assertEquals(MetricsKind.INCREMENT, point.kind());
        assertEquals("joystick.event", point.tags().get(EventMetricsObserver.TYPE_TAG));
        assertEquals(INSTANCE, point.tags().get(InstancedMetricsEmitter.INSTANCE_ID_TAG));
    }

    @Test
    void mqttOnQueuesWhenPublishThrows() {
        var emitter = AutonomyMetricsProducer.create(INSTANCE, true, (topic, payload, qos, retain) -> {
            throw new IllegalStateException("MQTT not connected");
        });
        emitter.increment(EventMetricsObserver.KEY, Map.of());
        emitter.increment(EventMetricsObserver.KEY, Map.of());
    }

    @Test
    void trueIsTheOnlyEnabledValue() {
        assertTrue(AutonomyMetricsProducer.mqttEnabled("true"));
        assertTrue(AutonomyMetricsProducer.mqttEnabled("TRUE"));
        assertFalse(AutonomyMetricsProducer.mqttEnabled("false"));
        assertFalse(AutonomyMetricsProducer.mqttEnabled("banana"));
        assertFalse(AutonomyMetricsProducer.mqttEnabled(null));
    }

    private record Published(String topic, byte[] payload, int qos, boolean retain) {}
}
