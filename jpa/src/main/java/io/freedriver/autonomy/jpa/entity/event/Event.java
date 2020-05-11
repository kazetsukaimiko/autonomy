package io.freedriver.autonomy.jpa.entity.event;

import io.freedriver.autonomy.jpa.entity.EntityBase;

import javax.persistence.Column;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * An entity that represents either the initial state of some thing or a change
 * in that thing's state.
 */
public abstract class Event extends EntityBase implements Serializable {
    public static final long serialVersionUID = -1L;

    @Temporal(TemporalType.TIMESTAMP)
    private Instant timestamp;

    @ManyToOne
    private EventCoordinate coordinate;

    @ManyToOne
    private EventDescription description;

    @Enumerated
    private SourceType sourceType;

    @Enumerated
    private EventPriority priority = EventPriority.STANDARD;

    @Column
    private EventAction action;

    protected Event() {
    }

    public Event(Instant timestamp, EventCoordinate coordinate, EventDescription description, SourceType sourceType, EventPriority priority) {
        this.timestamp = timestamp;
        this.coordinate = coordinate;
        this.description = description;
        this.sourceType = sourceType;
        this.priority = priority;
    }

    public Event(Instant timestamp, EventCoordinate coordinate, EventDescription description, SourceType sourceType) {
        this(timestamp, coordinate, description, sourceType, EventPriority.STANDARD);
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public EventCoordinate getCoordinate() {
        return coordinate;
    }

    public void setCoordinate(EventCoordinate coordinate) {
        this.coordinate = coordinate;
    }

    public EventDescription getDescription() {
        return description;
    }

    public void setDescription(EventDescription description) {
        this.description = description;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(SourceType sourceType) {
        this.sourceType = sourceType;
    }

    public EventPriority getPriority() {
        return priority;
    }

    public void setPriority(EventPriority priority) {
        this.priority = priority;
    }

    public EventAction getAction() {
        return action;
    }

    public void setAction(EventAction action) {
        this.action = action;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        return Objects.equals(timestamp, event.timestamp) &&
                Objects.equals(coordinate, event.coordinate) &&
                Objects.equals(description, event.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, coordinate, description);
    }

    @Override
    public String toString() {
        return "Event{" +
                ", timestamp=" + timestamp +
                ", coordinate=" + coordinate +
                ", description=" + description +
                '}';
    }
}
