package io.freedriver.autonomy.events.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class NoOpEventStoreTest {

    private final NoOpEventStore store = new NoOpEventStore();

    @Test
    void appendIsAcceptedAndDiscarded() {
        EventMetadata metadata = metadata("src-1", "evt-1");
        EventRecord<String> first = store.append("joystick.event", "first", metadata);
        store.append("joystick.event", "second", metadata("src-1", "evt-2"));

        assertNotNull(first.id());
        assertEquals("joystick.event", first.eventType());
        assertEquals("first", first.payload());
        assertEquals(metadata.timestamp(), first.timestamp());
        assertEquals("src-1", first.sourceId());
        assertEquals(0, store.query(EventQuery.byId("joystick.event", first.id())).count());
        assertEquals(0, store.query(EventQuery.all("joystick.event")).count());
        assertEquals(0, store.query(EventQuery.since("joystick.event", Instant.EPOCH, 10)).count());
        assertEquals(0, store.query(EventQuery.bySource("joystick.event", "src-1", Instant.EPOCH, 10)).count());
    }

    @Test
    void queriesWithOpenCriteriaAreEmpty() {
        store.append("speech.event", "hello", metadata("speaker", "line-1"));
        EventQuery open = new EventQuery(
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Integer.MAX_VALUE);
        assertEquals(0, store.query(open).count());
        assertEquals(0, store.query(null).count());
    }

    @Test
    void purgeLeavesTheStoreEmpty() {
        store.append("vedirect.message", "frame", metadata("serial", "frame-1"));
        assertEquals(0, store.purgeOlderThan(Instant.now()));
        assertEquals(0, store.purgeOlderThan(null));
        assertEquals(0, store.query(EventQuery.all("vedirect.message")).count());
    }

    private static EventMetadata metadata(String sourceId, String eventId) {
        return new EventMetadata(Instant.parse("2026-09-27T00:00:00Z"), "source", sourceId, eventId);
    }
}
