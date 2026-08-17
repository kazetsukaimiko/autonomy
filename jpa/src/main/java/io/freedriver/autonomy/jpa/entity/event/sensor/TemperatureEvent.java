package io.freedriver.autonomy.jpa.entity.event.sensor;

import io.freedriver.autonomy.jpa.entity.event.Event;
import io.freedriver.autonomy.jpa.entity.event.GenerationOrigin;
import io.freedriver.math.measurement.types.thermo.Temperature;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Table
@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
public class TemperatureEvent extends Event {

    @Column
    private Temperature temperature;

    public TemperatureEvent(long timestamp, GenerationOrigin generationOrigin, String sourceClass, String sourceId, String eventId, Temperature temperature) {
        super(timestamp, generationOrigin, sourceClass, sourceId, eventId);
        this.temperature = temperature;
    }
}
