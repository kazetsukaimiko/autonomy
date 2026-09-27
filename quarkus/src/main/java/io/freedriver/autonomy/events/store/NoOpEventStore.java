package io.freedriver.autonomy.events.store;

import java.time.Instant;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Application-scoped {@link EventStore} that accepts each append and answers every query with an empty result.
 */
@ApplicationScoped
public class NoOpEventStore implements EventStore {

    @Override
    public <T> EventRecord<T> append(String eventType, T payload, EventMetadata metadata) {
        return EventRecord.of(eventType, payload, metadata);
    }

    @Override
    public Stream<StoredEvent> query(EventQuery query) {
        return Stream.empty();
    }

    @Override
    public int purgeOlderThan(Instant cutoff) {
        return 0;
    }
}
