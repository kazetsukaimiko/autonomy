package io.freedriver.autonomy.events;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EventSourceTest {
    @Test
    void discoversRegisteredProviders() {
        assertTrue(
                EventSource.load().anyMatch(TestEventSource.class::isInstance),
                "expected TestEventSource from META-INF/services");
    }
}