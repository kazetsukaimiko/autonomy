package io.freedriver.autonomy.service.crud;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.stream.Stream;

import io.freedriver.autonomy.event.Event;
import io.freedriver.autonomy.events.store.EventMetadata;
import io.freedriver.autonomy.events.store.EventQuery;
import io.freedriver.autonomy.events.store.EventStore;
import io.freedriver.autonomy.events.store.StoredEvent;
import io.freedriver.autonomy.jaxrs.ObjectMapperContextResolver;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class EventCrudService<E extends Event> {

    @Inject
    EventStore eventStore;

    public abstract String eventType();

    public abstract Class<E> payloadType();

    public static Instant getStartOfDay() {
        LocalDateTime localDateTime = LocalDateTime.now().toLocalDate().atStartOfDay();
        return localDateTime.toInstant(ZoneOffset.UTC);
    }

    public static Instant ago(Duration duration) {
        return LocalDateTime.now().minus(duration).toInstant(ZoneOffset.UTC);
    }

    public Stream<E> fromStartOfDay() {
        return since(getStartOfDay(), payloadType().getSimpleName() + " for today");
    }

    public Stream<E> last(Duration duration) {
        return since(Instant.now().minus(duration), payloadType().getSimpleName() + " last " + duration.toMillis() + "ms");
    }

    public Stream<E> since(Instant instant, String message) {
        log.trace(message);
        return query(EventQuery.since(eventType(), instant, Integer.MAX_VALUE));
    }

    public Stream<E> query(EventQuery query) {
        return eventStore.query(query).map(this::readPayload);
    }

    public Optional<E> get(String id) {
        return eventStore.query(EventQuery.byId(eventType(), id)).findFirst().map(this::readPayload);
    }

    public int applyTTL(Duration duration) {
        log.trace("Applying TTL");
        return eventStore.purgeOlderThan(Instant.now().minus(duration));
    }

    protected E persist(E event) {
        EventMetadata metadata = new EventMetadata(
                event.timestamp(), event.sourceClass(), event.sourceId(), event.eventId());
        eventStore.append(eventType(), event, metadata);
        return event;
    }

    protected E readPayload(StoredEvent stored) {
        Object raw = stored.payload();
        if (payloadType().isInstance(raw)) {
            return payloadType().cast(raw);
        }
        return ObjectMapperContextResolver.getMapper().convertValue(raw, payloadType());
    }
}
