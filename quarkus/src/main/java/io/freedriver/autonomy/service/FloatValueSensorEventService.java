package io.freedriver.autonomy.service;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;

import io.freedriver.autonomy.jpa.entity.event.input.sensors.DoubleValueSensorEvent;
import io.freedriver.autonomy.service.crud.EventCrudService;

@ApplicationScoped
public class FloatValueSensorEventService extends EventCrudService<DoubleValueSensorEvent> {
    @Override
    public Class<DoubleValueSensorEvent> getEntityClass() {
        return DoubleValueSensorEvent.class;
    }

    /**
     * Saves a FloatValueSensorEvent.
     *
     * @param event
     * @return
     */
    @Transactional
    public DoubleValueSensorEvent save(DoubleValueSensorEvent event) {
        return persist(event);
    }
}
