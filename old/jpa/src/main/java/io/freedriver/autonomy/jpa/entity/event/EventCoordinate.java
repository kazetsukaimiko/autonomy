package io.freedriver.autonomy.jpa.entity.event;

import io.freedriver.autonomy.jpa.entity.EmbeddedEntityBase;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.util.Objects;

/**
 * This class seeks to describe where an event came from, both uniquely and nonuniquely.
 */
@Embeddable
public class EventCoordinate extends EmbeddedEntityBase {

    // What initiated this event
    @Enumerated(EnumType.STRING)
    private GenerationOrigin generationOrigin;

    // The type of the source.
    @Column(nullable = false)
    private String sourceClass;

    // The specific identifier for this source.
    @Column(nullable = true)
    private String sourceId;

    public EventCoordinate(EmbeddedEntityBase base, GenerationOrigin generationOrigin, String sourceClass, String sourceId) {
        super(base);
        this.generationOrigin = generationOrigin;
        this.sourceClass = sourceClass;
        this.sourceId = sourceId;
    }

    public EventCoordinate() {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventCoordinate that = (EventCoordinate) o;
        return generationOrigin == that.generationOrigin &&
                Objects.equals(sourceClass, that.sourceClass) &&
                Objects.equals(sourceId, that.sourceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(generationOrigin, sourceClass, sourceId);
    }

    @Override
    public String toString() {
        return "EventCoordinate{" +
                "generationOrigin=" + generationOrigin +
                ", sourceClass='" + sourceClass + '\'' +
                ", sourceId='" + sourceId + '\'' +
                '}';
    }
}
