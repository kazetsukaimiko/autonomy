package io.freedriver.autonomy.events.store;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import io.freedriver.autonomy.jaxrs.ObjectMapperContextResolver;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * MongoDB {@link EventStore}. A refused call is logged and does not escape to the caller.
 * One failure backs off so a down database does not stall every sensor, speech, and VE.Direct write.
 */
@ApplicationScoped
@Slf4j
public class MongoEventStore implements EventStore {

    static final String COLLECTION = "events";

    private static final Duration FAILURE_BACKOFF = Duration.ofSeconds(5);
    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {};

    private final MongoClient mongoClient;
    private final String databaseName;
    private final AtomicLong skipUntilMillis = new AtomicLong();

    @Inject
    public MongoEventStore(
            MongoClient mongoClient,
            @ConfigProperty(name = "quarkus.mongodb.database", defaultValue = "autonomy") String databaseName) {
        this.mongoClient = mongoClient;
        this.databaseName = databaseName;
    }

    @Override
    public <T> EventRecord<T> append(String eventType, T payload, EventMetadata metadata) {
        EventRecord<T> record = EventRecord.of(eventType, payload, metadata);
        if (!accepting()) {
            return record;
        }
        try {
            collection().insertOne(document(record));
            resume();
        } catch (RuntimeException e) {
            pauseAfter(e, "write");
        }
        return record;
    }

    @Override
    public Stream<StoredEvent> query(EventQuery query) {
        if (query == null || query.limit() <= 0 || !accepting()) {
            return Stream.empty();
        }
        try {
            List<Bson> filters = new ArrayList<>();
            query.eventType().ifPresent(type -> filters.add(Filters.eq("eventType", type)));
            query.since().ifPresent(since -> filters.add(Filters.gte("timestamp", Date.from(since))));
            query.sourceId().ifPresent(sourceId -> filters.add(Filters.eq("sourceId", sourceId)));
            query.id().ifPresent(id -> filters.add(Filters.eq("_id", id)));
            Bson filter = filters.isEmpty() ? new Document() : Filters.and(filters);
            var find = collection().find(filter).sort(Sorts.descending("timestamp"));
            if (query.limit() < Integer.MAX_VALUE) {
                find = find.limit(query.limit());
            }
            List<StoredEvent> events = new ArrayList<>();
            try (MongoCursor<Document> cursor = find.iterator()) {
                while (cursor.hasNext()) {
                    events.add(toStored(cursor.next()));
                }
            }
            resume();
            return events.stream();
        } catch (RuntimeException e) {
            pauseAfter(e, "read");
            return Stream.empty();
        }
    }

    @Override
    public int purgeOlderThan(Instant cutoff) {
        if (cutoff == null || !accepting()) {
            return 0;
        }
        try {
            long deleted = collection()
                    .deleteMany(Filters.lt("timestamp", Date.from(cutoff)))
                    .getDeletedCount();
            resume();
            if (deleted > Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
            return (int) deleted;
        } catch (RuntimeException e) {
            pauseAfter(e, "purge");
            return 0;
        }
    }

    private MongoCollection<Document> collection() {
        return mongoClient.getDatabase(databaseName).getCollection(COLLECTION);
    }

    private boolean accepting() {
        return System.currentTimeMillis() >= skipUntilMillis.get();
    }

    private void resume() {
        skipUntilMillis.set(0);
    }

    private void pauseAfter(RuntimeException failure, String action) {
        skipUntilMillis.set(System.currentTimeMillis() + FAILURE_BACKOFF.toMillis());
        log.warn("Mongo event " + action + " failed: " + failure.getClass().getName() + ": " + failure.getMessage());
    }

    private static Document document(EventRecord<?> record) {
        Document document = new Document()
                .append("_id", record.id())
                .append("eventType", record.eventType())
                .append("sourceClass", record.sourceClass())
                .append("sourceId", record.sourceId())
                .append("eventId", record.eventId())
                .append("payload", bsonValue(payloadValue(record.payload())));
        if (record.timestamp() != null) {
            document.append("timestamp", Date.from(record.timestamp()));
        }
        return document;
    }

    private static Object payloadValue(Object payload) {
        if (payload == null || payload instanceof String || payload instanceof Number || payload instanceof Boolean) {
            return payload;
        }
        if (payload instanceof Map<?, ?> || payload instanceof List<?>) {
            return payload;
        }
        return ObjectMapperContextResolver.getMapper().convertValue(payload, MAP);
    }

    private static Object bsonValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Document document = new Document();
            map.forEach((key, nested) -> document.append(String.valueOf(key), bsonValue(nested)));
            return document;
        }
        if (value instanceof List<?> list) {
            List<Object> converted = new ArrayList<>();
            for (Object item : list) {
                converted.add(bsonValue(item));
            }
            return converted;
        }
        return value;
    }

    private static StoredEvent toStored(Document document) {
        return new StoredEvent(
                string(document.get("_id")),
                document.getString("eventType"),
                instant(document.get("timestamp")),
                document.getString("sourceClass"),
                document.getString("sourceId"),
                document.getString("eventId"),
                payloadMap(document.get("payload")));
    }

    private static Map<String, Object> payloadMap(Object raw) {
        if (raw == null) {
            return Map.of();
        }
        if (raw instanceof Map<?, ?>) {
            return ObjectMapperContextResolver.getMapper().convertValue(raw, MAP);
        }
        Map<String, Object> wrapped = new LinkedHashMap<>();
        wrapped.put("value", raw);
        return wrapped;
    }

    private static Instant instant(Object raw) {
        if (raw instanceof Date date) {
            return date.toInstant();
        }
        if (raw instanceof Instant instant) {
            return instant;
        }
        return null;
    }

    private static String string(Object raw) {
        return raw == null ? null : raw.toString();
    }
}
