package io.freedriver.autonomy.service;

import io.freedriver.autonomy.jpa.entity.event.input.sensors.DoubleValueSensorEvent;
import io.freedriver.autonomy.service.crud.EventCrudService;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;

@ApplicationScoped
public class DoubleValueSensorEventService extends EventCrudService<DoubleValueSensorEvent> {
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
