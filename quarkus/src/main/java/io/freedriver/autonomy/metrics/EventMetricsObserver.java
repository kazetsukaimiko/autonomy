package io.freedriver.autonomy.metrics;

import java.util.Map;

import io.freedriver.autonomy.event.input.joystick.JoystickEvent;
import io.freedriver.autonomy.event.input.sensors.DoubleValueSensorEvent;
import io.freedriver.autonomy.event.speech.SpeechEvent;
import io.freedriver.autonomy.events.store.EventTypes;
import io.freedriver.metrics.MetricsEmitter;
import io.freedriver.victron.VEDirectMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * Emits {@code autonomy.event} for every EventTypes payload that reaches the CDI
 * bus. Joystick and VEDirect sources fireAsync; speech still uses fire, so that
 * observer is synchronous. Sensor float is fired async from the persist
 * chokepoint.
 */
@ApplicationScoped
@Slf4j
public class EventMetricsObserver {

    static final String KEY = "autonomy.event";
    static final String TYPE_TAG = "type";

    private final MetricsEmitter emitter;

    @Inject
    public EventMetricsObserver(MetricsEmitter emitter) {
        this.emitter = emitter;
    }

    public void onJoystick(@ObservesAsync @Default JoystickEvent event) {
        emit(EventTypes.JOYSTICK_EVENT);
    }

    public void onVeDirect(@ObservesAsync @Default VEDirectMessage event) {
        emit(EventTypes.VEDIRECT_MESSAGE);
    }

    public void onSpeech(@Observes SpeechEvent event) {
        emit(EventTypes.SPEECH_EVENT);
    }

    public void onSensor(@ObservesAsync @Default DoubleValueSensorEvent event) {
        emit(EventTypes.SENSOR_FLOAT);
    }

    void emit(String type) {
        try {
            emitter.increment(KEY, Map.of(TYPE_TAG, type));
        } catch (RuntimeException e) {
            log.debug("metrics emit failed type={}", type, e);
        }
    }
}
