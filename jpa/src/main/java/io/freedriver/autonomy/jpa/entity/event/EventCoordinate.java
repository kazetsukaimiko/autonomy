package io.freedriver.autonomy.jpa.entity.event;

import io.freedriver.autonomy.jpa.entity.EmbeddedEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * This class seeks to describe where an event came from, both uniquely and nonuniquely.
 */
@Embeddable
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
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
}
