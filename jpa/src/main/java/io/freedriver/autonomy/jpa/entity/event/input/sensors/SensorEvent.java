package io.freedriver.autonomy.jpa.entity.event.input.sensors;

import java.util.UUID;

import io.freedriver.autonomy.jpa.entity.event.Event;
import io.freedriver.autonomy.jpa.entity.event.GenerationOrigin;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@MappedSuperclass
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
public abstract class SensorEvent extends Event {
    @Column
    private UUID boardId;

    @Column
    private String sensorName;

    public SensorEvent(UUID boardId, String sensorName) {
        this.boardId = boardId;
        this.sensorName = sensorName;
    }

    public SensorEvent(long timestamp, GenerationOrigin generationOrigin, String sourceClass, String sourceId, String eventId, UUID boardId, String sensorName) {
        super(timestamp, generationOrigin, sourceClass, sourceId, eventId);
        this.boardId = boardId;
        this.sensorName = sensorName;
    }
}
