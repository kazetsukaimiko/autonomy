package io.freedriver.autonomy.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.freedriver.autonomy.events.store.EventTypes;
import io.freedriver.metrics.MetricsEmitter;
import io.freedriver.metrics.MetricsKind;
import io.freedriver.metrics.MetricsPoint;
import org.junit.jupiter.api.Test;

class EventMetricsObserverTest {

    @Test
    void eachEventTypeIncrementsAutonomyEvent() {
        RecordingEmitter emitter = new RecordingEmitter();
        EventMetricsObserver observer = new EventMetricsObserver(emitter);
        observer.onJoystick(null);
        observer.onVeDirect(null);
        observer.onSpeech(null);
        observer.onSensor(null);
        assertEquals(
                List.of(
                        EventTypes.JOYSTICK_EVENT,
                        EventTypes.VEDIRECT_MESSAGE,
                        EventTypes.SPEECH_EVENT,
                        EventTypes.SENSOR_FLOAT),
                emitter.points.stream().map(point -> point.tags().get(EventMetricsObserver.TYPE_TAG)).toList());
        assertTrue(emitter.points.stream().allMatch(point -> EventMetricsObserver.KEY.equals(point.key())));
        assertTrue(emitter.points.stream().allMatch(point -> point.kind() == MetricsKind.INCREMENT));
    }

    @Test
    void aThrowingEmitterDoesNotEscapeTheObserver() {
        EventMetricsObserver observer = new EventMetricsObserver(new MetricsEmitter() {
            @Override
            public void increment(String key, Map<String, Object> tags) {
                throw new IllegalStateException("broker down");
            }

            @Override
            public void count(String key, Map<String, Object> tags) {}

            @Override
            public void gauge(String key, Map<String, Object> tags) {}
        });
        observer.onJoystick(null);
        observer.onSpeech(null);
    }

    private static final class RecordingEmitter implements MetricsEmitter {
        private final List<MetricsPoint> points = new ArrayList<>();

        @Override
        public void increment(String key, Map<String, Object> tags) {
            points.add(MetricsPoint.increment(key, tags));
        }

        @Override
        public void count(String key, Map<String, Object> tags) {
            points.add(MetricsPoint.count(key, tags));
        }

        @Override
        public void gauge(String key, Map<String, Object> tags) {
            points.add(MetricsPoint.gauge(key, tags));
        }
    }
}
