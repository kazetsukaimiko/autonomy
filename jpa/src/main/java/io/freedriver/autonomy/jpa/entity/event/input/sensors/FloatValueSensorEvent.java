package io.freedriver.autonomy.jpa.entity.event.input.sensors;


import io.freedriver.autonomy.jpa.entity.event.GenerationOrigin;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table
@Inheritance(strategy = InheritanceType.JOINED)
public class FloatValueSensorEvent extends SensorEvent {
    @Column
    private float value;

    public FloatValueSensorEvent() {
    }

    public FloatValueSensorEvent(UUID boardId, String sensorName, float value) {
        super(boardId, sensorName);
        this.value = value;
    }

    public FloatValueSensorEvent(long timestamp, GenerationOrigin generationOrigin, String sourceClass, String sourceId, String eventId, UUID boardId, String sensorName, float value) {
        super(timestamp, generationOrigin, sourceClass, sourceId, eventId, boardId, sensorName);
        this.value = value;
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "FloatValueSensorEvent{" +
                "value=" + value +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        FloatValueSensorEvent that = (FloatValueSensorEvent) o;
        return Float.compare(that.value, value) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), value);
    }
}
