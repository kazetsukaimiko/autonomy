package io.freedriver.autonomy.jpa.entity.event.input.sensors;

import io.freedriver.autonomy.jpa.entity.event.Event;
import io.freedriver.autonomy.jpa.entity.event.GenerationOrigin;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import java.util.Objects;
import java.util.UUID;

@MappedSuperclass
public abstract class SensorEvent extends Event {
    @Column
    private UUID boardId;

    @Column
    private String sensorName;

    public SensorEvent() {
    }

    public SensorEvent(UUID boardId, String sensorName) {
        this.boardId = boardId;
        this.sensorName = sensorName;
    }

    public SensorEvent(long timestamp, GenerationOrigin generationOrigin, String sourceClass, String sourceId, String eventId, UUID boardId, String sensorName) {
        super(timestamp, generationOrigin, sourceClass, sourceId, eventId);
        this.boardId = boardId;
        this.sensorName = sensorName;
    }

    public UUID getBoardId() {
        return boardId;
    }

    public void setBoardId(UUID boardId) {
        this.boardId = boardId;
    }

    public String getSensorName() {
        return sensorName;
    }

    public void setSensorName(String sensorName) {
        this.sensorName = sensorName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SensorEvent that = (SensorEvent) o;
        return Objects.equals(boardId, that.boardId) && Objects.equals(sensorName, that.sensorName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), boardId, sensorName);
    }
}
