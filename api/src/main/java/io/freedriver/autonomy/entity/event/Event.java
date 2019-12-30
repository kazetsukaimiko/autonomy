package io.freedriver.autonomy.entity.event;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.dizitart.no2.IndexType;
import org.dizitart.no2.NitriteId;
import org.dizitart.no2.objects.Id;
import org.dizitart.no2.objects.Index;
import org.dizitart.no2.objects.Indices;

import java.time.Instant;
import java.util.Objects;

/**
 * An entity that represents either the initial state of some thing or a change
 * in that thing's state.
 */
@Indices({
        @Index(value="timestamp", type= IndexType.NonUnique),
        @Index(value="subject", type= IndexType.NonUnique),
        @Index(value="property", type= IndexType.NonUnique),
        @Index(value="type", type= IndexType.NonUnique)
})
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY, property="@extension")
public abstract class Event {

    @Id
    private NitriteId id;

    private Instant timestamp;

    @JsonUnwrapped
    private EventCoordinate coordinate;

    @JsonUnwrapped
    private EventDescription description;

    protected Event() {
    }

    protected Event(Instant timestamp, EventCoordinate coordinate, EventDescription description) {
        this.timestamp = timestamp;
        this.coordinate = coordinate;
        this.description = description;
    }

    public NitriteId getId() {
        return id;
    }

    public void setId(NitriteId id) {
        this.id = id;
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
