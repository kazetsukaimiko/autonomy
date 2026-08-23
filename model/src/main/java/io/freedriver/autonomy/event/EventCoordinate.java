package io.freedriver.autonomy.event;

/**
 * Where an event came from, both uniquely and non-uniquely.
 */
public record EventCoordinate(
        long position, GenerationOrigin generationOrigin, String sourceClass, String sourceId) {}
