package io.freedriver.autonomy.events.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Instant;
import java.util.UUID;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import io.freedriver.autonomy.MappingsTestResource;
import io.freedriver.autonomy.event.GenerationOrigin;
import io.freedriver.autonomy.event.speech.SpeechEvent;
import io.freedriver.autonomy.event.speech.SpeechEventType;
import io.freedriver.autonomy.jaxrs.ObjectMapperContextResolver;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.bson.Document;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@QuarkusTest
@QuarkusTestResource(MappingsTestResource.class)
@QuarkusTestResource(value = MongoEventStoreResource.class, restrictToAnnotatedClass = true)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MongoEventStoreTest {

    private static final String DATABASE = "autonomy";

    @Inject
    MongoEventStore store;

    @Inject
    MongoClient mongoClient;

    @Test
    @Order(1)
    void storeStartsEmpty() {
        assumeDocker();
        assertEquals(0, events().countDocuments());
    }

    @Test
    @Order(2)
    void writtenEventIsReadBackFromMongo() {
        assumeDocker();
        SpeechEvent speech = speech("hello from the store");
        EventMetadata metadata = new EventMetadata(
                speech.timestamp(), speech.sourceClass(), speech.sourceId(), speech.eventId());
        EventRecord<SpeechEvent> written = store.append(EventTypes.SPEECH_EVENT, speech, metadata);

        Document document = events().find(new Document("_id", written.id())).first();
        assertNotNull(document);
        assertEquals(EventTypes.SPEECH_EVENT, document.getString("eventType"));
        assertEquals(speech.sourceId(), document.getString("sourceId"));
        assertNoStoredClassName(document);
        Document payload = document.get("payload", Document.class);
        assertNotNull(payload);
        assertNoStoredClassName(payload);
        assertFalse(payload.containsKey("className"));
        assertEquals("hello from the store", payload.getString("text"));

        StoredEvent stored = store.query(EventQuery.byId(EventTypes.SPEECH_EVENT, written.id()))
                .findFirst()
                .orElseThrow();
        SpeechEvent read = ObjectMapperContextResolver.getMapper().convertValue(stored.payload(), SpeechEvent.class);
        assertEquals(speech.text(), read.text());
        assertEquals(speech.subject(), read.subject());
        assertEquals(speech.speechEventType(), read.speechEventType());
        assertEquals(0, store.query(EventQuery.since(EventTypes.SPEECH_EVENT, Instant.now().plusSeconds(60), 10))
                .filter(event -> written.id().equals(event.id()))
                .count());
        assertEquals(1, store.query(EventQuery.bySource(
                        EventTypes.SPEECH_EVENT, speech.sourceId(), Instant.EPOCH, 10))
                .count());
    }

    @Test
    @Order(3)
    void purgeRemovesOnlyOlderEvents() {
        assumeDocker();
        SpeechEvent kept = speech("keep");
        EventRecord<SpeechEvent> written = store.append(
                EventTypes.SPEECH_EVENT,
                kept,
                new EventMetadata(kept.timestamp(), kept.sourceClass(), kept.sourceId(), kept.eventId()));
        assertNotNull(events().find(new Document("_id", written.id())).first());

        store.purgeOlderThan(Instant.now().minusSeconds(60));
        assertNotNull(events().find(new Document("_id", written.id())).first());

        store.purgeOlderThan(Instant.now().plusSeconds(60));
        assertNull(events().find(new Document("_id", written.id())).first());
    }

    private MongoCollection<Document> events() {
        return mongoClient.getDatabase(DATABASE).getCollection(MongoEventStore.COLLECTION);
    }

    private static void assumeDocker() {
        if (MongoEventStoreResource.started) {
            return;
        }
        if ("true".equals(System.getenv("CI")) || "true".equals(System.getenv("GITHUB_ACTIONS"))) {
            fail("MongoDB test container did not start");
        }
        Assumptions.assumeTrue(false, "MongoDB test container needs Docker");
    }

    private static SpeechEvent speech(String text) {
        return new SpeechEvent(
                UUID.randomUUID().toString(),
                Instant.parse("2026-10-21T12:00:00Z"),
                GenerationOrigin.NON_HUMAN,
                "test.Source",
                "speaker-" + UUID.randomUUID(),
                "evt-" + UUID.randomUUID(),
                SpeechEventType.IMPERATIVE,
                "subject",
                text);
    }

    private static void assertNoStoredClassName(Document document) {
        assertNull(document.get("@class"));
        assertNull(document.get("_class"));
        assertNull(document.get("className"));
        assertFalse(document.toJson().contains("io.freedriver.autonomy.event.speech.SpeechEvent"));
    }
}
