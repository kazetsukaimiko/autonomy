package io.freedriver.autonomy.events.store;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Mongo-backed unified events collection. Wired once quarkus-mongodb-panache is enabled in runtime config.
 */
@ApplicationScoped
public class MongoEventStore implements EventStore {
    @Inject
    ObjectMapper objectMapper;

    @Override
    public <T> String append(String eventType, T payload, EventMetadata metadata) {
        String id = UUID.randomUUID().toString();
        Map<String, Object> document = new HashMap<>();
        document.put("_id", id);
        document.put("eventType", eventType);
        document.put("timestamp", metadata.timestamp());
        document.put("sourceClass", metadata.sourceClass());
        document.put("sourceId", metadata.sourceId());
        document.put("eventId", metadata.eventId());
        document.put("payload", objectMapper.convertValue(payload, Map.class));
        // TODO: persist document via Panache Mongo repository once datasource is configured.
        return id;
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