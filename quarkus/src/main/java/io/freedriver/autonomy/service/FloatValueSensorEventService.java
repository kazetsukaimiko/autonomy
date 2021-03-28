package io.freedriver.autonomy.service;

import io.freedriver.autonomy.jpa.entity.VEDirectMessage;
import io.freedriver.autonomy.jpa.entity.event.input.sensors.FloatValueSensorEvent;
import io.freedriver.autonomy.service.crud.EventCrudService;

import javax.transaction.Transactional;

public class FloatValueSensorEventService extends EventCrudService<FloatValueSensorEvent> {
    @Override
    public Class<FloatValueSensorEvent> getEntityClass() {
        return FloatValueSensorEvent.class;
    }

    /**
     * Saves a FloatValueSensorEvent.
     *
     * @param event
     * @return
     */
    @Transactional
    public FloatValueSensorEvent save(FloatValueSensorEvent event) {
        return persist(event);
    }
}
