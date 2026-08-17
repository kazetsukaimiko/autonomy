package io.freedriver.autonomy.jpa.entity.event;

import io.freedriver.autonomy.jpa.entity.EntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * An entity that represents either the initial state of some thing or a change
 * in that thing's state.
 */
@MappedSuperclass
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
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

    protected Event(long timestamp, GenerationOrigin generationOrigin, String sourceClass, String sourceId, String eventId) {
        super();
        this.timestamp = timestamp;
        this.generationOrigin = generationOrigin;
        this.sourceClass = sourceClass;
        this.sourceId = sourceId;
        this.eventId = eventId;
    }
}
