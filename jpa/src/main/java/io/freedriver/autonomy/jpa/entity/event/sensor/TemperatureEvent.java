package io.freedriver.autonomy.jpa.entity.event.sensor;

import io.freedriver.autonomy.jpa.entity.event.Event;
import io.freedriver.autonomy.jpa.entity.event.GenerationOrigin;
import kaze.math.measurement.types.thermo.Temperature;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Objects;

@Table
@Entity
public class TemperatureEvent extends Event {

    @Column
    private Temperature temperature;

    public TemperatureEvent() {
    }

    public TemperatureEvent(long timestamp, GenerationOrigin generationOrigin, String sourceClass, String sourceId, String eventId, Temperature temperature) {
        super(timestamp, generationOrigin, sourceClass, sourceId, eventId);
        this.temperature = temperature;
    }

    public Temperature getTemperature() {
        return temperature;
    }

    public void setTemperature(Temperature temperature) {
        this.temperature = temperature;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        TemperatureEvent that = (TemperatureEvent) o;
        return Objects.equals(temperature, that.temperature);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), temperature);
    }

    @Override
    public String toString() {
        return "TemperatureEvent{" +
                "temperature=" + temperature +
                '}';
    }
}
