package io.freedriver.autonomy.service;

import io.freedriver.autonomy.event.input.sensors.DoubleValueSensorEvent;
import io.freedriver.autonomy.events.store.EventTypes;
import io.freedriver.autonomy.service.crud.EventCrudService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class FloatValueSensorEventService extends EventCrudService<DoubleValueSensorEvent> {

    @Inject
    Event<DoubleValueSensorEvent> sensorEvents;

    @Override
    public String eventType() {
        return EventTypes.SENSOR_FLOAT;
    }

    @Override
    public Class<DoubleValueSensorEvent> payloadType() {
        return DoubleValueSensorEvent.class;
    }

    public DoubleValueSensorEvent save(DoubleValueSensorEvent event) {
        DoubleValueSensorEvent saved = persist(event);
        sensorEvents.fireAsync(saved).whenComplete((ignored, error) -> {
            if (error != null) {
                log.warn("Failed to fire DoubleValueSensorEvent: {}", saved, error);
            }
        });
        return saved;
    }
}
