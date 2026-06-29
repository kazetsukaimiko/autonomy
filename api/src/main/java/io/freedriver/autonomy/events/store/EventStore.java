package io.freedriver.autonomy.events.store;

import java.time.Instant;
import java.util.ServiceLoader;
import java.util.stream.Stream;

/**
 * Unified append/query store for autonomy events.
 *
 * <p>Register providers via {@code META-INF/services/} and discover them with {@link #load()}.
 */
public interface EventStore {
    <T> EventRecord<T> append(String eventType, T payload, EventMetadata metadata);

    Stream<StoredEvent> query(EventQuery query);

    int purgeOlderThan(Instant cutoff);

    /**
     * Discovers registered {@link EventStore} providers.
     *
     * <p>Uses this interface's defining class loader, not the current thread context class loader.
     */
    static Stream<EventStore> load() {
        return ServiceLoader.load(EventStore.class, EventStore.class.getClassLoader())
                .stream()
                .map(ServiceLoader.Provider::get);
    }
}