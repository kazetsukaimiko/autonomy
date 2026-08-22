package io.freedriver.autonomy.events.store;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.type.TypeReference;
import io.freedriver.autonomy.jaxrs.ObjectMapperContextResolver;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

/**
 * Unified events collection. Holds an in-process buffer so queries work without mongod;
 * Panache persist is the next slice once a connection string is configured.
 */
@ApplicationScoped
@Slf4j
public class MongoEventStore implements EventStore {

    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {};

    private final List<StoredEvent> events = new CopyOnWriteArrayList<>();

    @Override
    public <T> EventRecord<T> append(String eventType, T payload, EventMetadata metadata) {
        EventRecord<T> record = EventRecord.of(eventType, payload, metadata);
        events.add(toStored(record));
        return record;
    }

    @Override
    public Stream<StoredEvent> query(EventQuery query) {
        Stream<StoredEvent> stream = events.stream()
                .filter(event -> query.eventType().map(type -> Objects.equals(type, event.eventType())).orElse(true))
                .filter(event -> query.since().map(since -> !event.timestamp().isBefore(since)).orElse(true))
                .filter(event -> query.sourceId().map(sourceId -> Objects.equals(sourceId, event.sourceId())).orElse(true))
                .filter(event -> query.id().map(id -> Objects.equals(id, event.id())).orElse(true))
                .sorted(Comparator.comparing(StoredEvent::timestamp).reversed());
        if (query.limit() > 0 && query.limit() < Integer.MAX_VALUE) {
            stream = stream.limit(query.limit());
        }
        return stream;
    }

    @Override
    public int purgeOlderThan(Instant cutoff) {
        List<StoredEvent> stale = new ArrayList<>();
        for (StoredEvent event : events) {
            if (event.timestamp().isBefore(cutoff)) {
                stale.add(event);
            }
        }
        events.removeAll(stale);
        return stale.size();
    }

    private static StoredEvent toStored(EventRecord<?> record) {
        Map<String, Object> payload = ObjectMapperContextResolver.getMapper().convertValue(record.payload(), MAP);
        return new StoredEvent(
                record.id(),
                record.eventType(),
                record.timestamp(),
                record.sourceClass(),
                record.sourceId(),
                record.eventId(),
                payload);
    }
}
