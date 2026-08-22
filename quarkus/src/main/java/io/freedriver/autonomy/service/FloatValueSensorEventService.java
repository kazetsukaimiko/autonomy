package io.freedriver.autonomy.service;

import io.freedriver.autonomy.event.input.sensors.DoubleValueSensorEvent;
import io.freedriver.autonomy.events.store.EventTypes;
import io.freedriver.autonomy.service.crud.EventCrudService;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class FloatValueSensorEventService extends EventCrudService<DoubleValueSensorEvent> {
    @Override
    public String eventType() {
        return EventTypes.SENSOR_FLOAT;
    }

    @Override
    public Class<DoubleValueSensorEvent> payloadType() {
        return DoubleValueSensorEvent.class;
    }

    public DoubleValueSensorEvent save(DoubleValueSensorEvent event) {
        return persist(event);
    }
}
