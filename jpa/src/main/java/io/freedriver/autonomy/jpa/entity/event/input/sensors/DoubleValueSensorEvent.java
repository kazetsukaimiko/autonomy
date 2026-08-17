package io.freedriver.autonomy.jpa.entity.event.input.sensors;


import java.util.UUID;

import io.freedriver.autonomy.jpa.entity.event.GenerationOrigin;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
public class DoubleValueSensorEvent extends SensorEvent {
    @Column(name = "event_value")
    private double eventValue;

    public DoubleValueSensorEvent(UUID boardId, String sensorName, double eventValue) {
        super(boardId, sensorName);
        this.eventValue = eventValue;
    }

    public DoubleValueSensorEvent(long timestamp, GenerationOrigin generationOrigin, String sourceClass, String sourceId, String eventId, UUID boardId, String sensorName, double eventValue) {
        super(timestamp, generationOrigin, sourceClass, sourceId, eventId, boardId, sensorName);
        this.eventValue = eventValue;
    }
}
