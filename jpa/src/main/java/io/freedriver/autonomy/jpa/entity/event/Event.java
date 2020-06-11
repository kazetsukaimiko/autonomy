package io.freedriver.autonomy.jpa.entity.event;

import io.freedriver.autonomy.jpa.entity.EntityBase;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.MappedSuperclass;
import java.util.Objects;

/**
 * An entity that represents either the initial state of some thing or a change
 * in that thing's state.
 */
@MappedSuperclass
public abstract class Event extends EntityBase {
    public static final long serialVersionUID = -1L;

    private long timestamp;

    // What initiated this event
    @Enumerated(EnumType.STRING)
    private GenerationOrigin generationOrigin;

    // The type of the source.
    @Column(nullable = false)
    private String sourceClass;

    // The specific identifier for this source.
    @Column(nullable = false)
    private String sourceId;

    @Column(nullable = true)
    private String eventId;

    protected Event() {

    }

    protected Event(long timestamp, GenerationOrigin generationOrigin, String sourceClass, String sourceId, String eventId) {
        super();
        this.timestamp = timestamp;
        this.generationOrigin = generationOrigin;
        this.sourceClass = sourceClass;
        this.sourceId = sourceId;
        this.eventId = eventId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public GenerationOrigin getGenerationOrigin() {
        return generationOrigin;
    }

    public void setGenerationOrigin(GenerationOrigin generationOrigin) {
        this.generationOrigin = generationOrigin;
    }

    public String getSourceClass() {
        return sourceClass;
    }

    public void setSourceClass(String sourceClass) {
        this.sourceClass = sourceClass;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Event event = (Event) o;
        return timestamp == event.timestamp &&
                generationOrigin == event.generationOrigin &&
                Objects.equals(sourceClass, event.sourceClass) &&
                Objects.equals(sourceId, event.sourceId) &&
                Objects.equals(eventId, event.eventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), timestamp, generationOrigin, sourceClass, sourceId, eventId);
    }

    @Override
    public String toString() {
        return "Event{" +
                "timestamp=" + timestamp +
                ", generationOrigin=" + generationOrigin +
                ", sourceClass='" + sourceClass + '\'' +
                ", sourceId='" + sourceId + '\'' +
                ", eventId='" + eventId + '\'' +
                '}';
    }
}
