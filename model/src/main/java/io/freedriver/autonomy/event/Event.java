package io.freedriver.autonomy.event;

import java.time.Instant;

import io.freedriver.autonomy.entity.EntityBase;

/**
 * Either the initial state of some thing or a change in that thing's state.
 */
public interface Event extends EntityBase {
    Instant timestamp();

    GenerationOrigin generationOrigin();

    String sourceClass();

    String sourceId();

    String eventId();
}
