package io.freedriver.autonomy.events.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import com.fasterxml.jackson.core.type.TypeReference;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import io.freedriver.autonomy.event.GenerationOrigin;
import io.freedriver.autonomy.event.speech.SpeechEvent;
import io.freedriver.autonomy.event.speech.SpeechEventType;
import io.freedriver.autonomy.jaxrs.ObjectMapperContextResolver;
import org.jboss.logmanager.Level;
import org.jboss.logmanager.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MongoEventStoreFailureTest {

    private MongoClient client;
    private MongoEventStore store;
    private Logger logger;
    private Handler handler;
    private final List<String> messages = new ArrayList<>();

    @BeforeEach
    void setUp() {
        client = MongoClients.create(MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString("mongodb://127.0.0.1:1"))
                .applyToClusterSettings(builder -> builder.serverSelectionTimeout(200, TimeUnit.MILLISECONDS))
                .applyToSocketSettings(builder -> builder.connectTimeout(200, TimeUnit.MILLISECONDS))
                .build());
        store = new MongoEventStore(client, "autonomy");
        logger = Logger.getLogger(MongoEventStore.class.getName());
        logger.setLevel(Level.WARN);
        handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                messages.add(record.getMessage());
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        logger.addHandler(handler);
    }

    @AfterEach
    void tearDown() {
        logger.removeHandler(handler);
        client.close();
    }

    @Test
    void speechPayloadRoundTripsWithoutAClassName() {
        SpeechEvent speech = new SpeechEvent(
                UUID.randomUUID().toString(),
                Instant.parse("2026-10-21T12:00:00Z"),
                GenerationOrigin.NON_HUMAN,
                "test.Source",
                "speaker",
                "evt",
                SpeechEventType.IMPERATIVE,
                "subject",
                "hello from the store");
        Map<String, Object> payload = ObjectMapperContextResolver.getMapper()
                .convertValue(speech, new TypeReference<Map<String, Object>>() {});
        assertTrue(payload.keySet().stream().noneMatch(key -> key.equals("@class") || key.equals("_class") || key.equals("className")));
        assertTrue(payload.values().stream().noneMatch(value -> String.valueOf(value).contains("SpeechEvent")));
        SpeechEvent read = ObjectMapperContextResolver.getMapper().convertValue(payload, SpeechEvent.class);
        assertEquals(speech.text(), read.text());
        assertEquals(speech.speechEventType(), read.speechEventType());
        assertEquals(speech.timestamp(), read.timestamp());
    }

    @Test
    void failedWriteIsLoggedAndDoesNotThrow() {
        EventMetadata metadata = metadata("src-1", "evt-1");
        EventRecord<String> written = store.append("speech.event", "secret-text", metadata);

        assertNotNull(written.id());
        assertEquals("speech.event", written.eventType());
        assertEquals("secret-text", written.payload());
        assertTrue(messages.stream().anyMatch(message -> message.startsWith("Mongo event write failed:")));
        assertTrue(messages.stream().noneMatch(message -> message != null && message.contains("secret-text")));
        assertEquals(0, store.query(EventQuery.all("speech.event")).count());
        assertEquals(0, store.purgeOlderThan(Instant.now()));
        assertEquals(0, store.query(null).count());
        assertEquals(0, store.purgeOlderThan(null));
    }

    @Test
    void laterCallDoesNotWaitOnADownDatabase() {
        store.append("speech.event", "first", metadata("src-1", "evt-1"));
        assertTimeoutPreemptively(Duration.ofMillis(150), () -> {
            store.append("vedirect.message", "second", metadata("serial", "frame-1"));
            store.query(EventQuery.all("vedirect.message")).count();
        });
    }

    private static EventMetadata metadata(String sourceId, String eventId) {
        return new EventMetadata(Instant.parse("2026-10-21T00:00:00Z"), "source", sourceId, eventId);
    }
}
