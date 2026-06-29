package io.freedriver.autonomy.events.store;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EventStoreTest {
    @Test
    void discoversRegisteredProviders() {
        assertTrue(
                EventStore.load().anyMatch(TestEventStore.class::isInstance),
                "expected TestEventStore from META-INF/services");
    }
}