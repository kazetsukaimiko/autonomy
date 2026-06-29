package io.freedriver.autonomy.events.store;

import java.time.Instant;
import java.util.stream.Stream;

public final class TestEventStore implements EventStore {
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