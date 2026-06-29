package io.freedriver.autonomy.events.store;

import java.time.Instant;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Mongo-backed unified events collection. Wired once quarkus-mongodb-panache is enabled in runtime config.
 */
@ApplicationScoped
public class MongoEventStore implements EventStore {

    @Override
    public <T> EventRecord<T> append(String eventType, T payload, EventMetadata metadata) {
        EventRecord<T> record = EventRecord.of(eventType, payload, metadata);
        // TODO: persist record via Panache Mongo repository once datasource is configured.
        return record;
    }

    @Override
    public Stream<StoredEvent> query(EventQuery query) {
        // TODO: query Mongo events collection by eventType/timestamp/sourceId.
        return Stream.empty();
    }

    @Override
    public int purgeOlderThan(Instant cutoff) {
        // TODO: deleteMany where timestamp < cutoff.
        return 0;
    }
}