package io.freedriver.autonomy.service;

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import io.freedriver.autonomy.event.GenerationOrigin;
import io.freedriver.autonomy.event.input.joystick.JoystickEvent;
import io.freedriver.autonomy.event.input.joystick.JoystickEventType;
import io.freedriver.autonomy.events.store.MongoEventStore;
import io.freedriver.autonomy.service.crud.EventCrudService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Button 11's mapped key is {@code 11:0}, so the hallway action runs on release.
 * The reader thread fires the press line and does not read the release until
 * {@link JoystickEventCrudService#actOnJoystickEvent} returns. A refused Mongo
 * write waits out {@code quarkus.mongodb.server-selection-timeout} (1s) before
 * the backoff starts, which holds that release.
 */
class HallwayReleaseStallTest {

    private static final Duration SELECTION_TIMEOUT = Duration.ofSeconds(1);
    private static final Duration RELEASE_BUDGET = Duration.ofMillis(300);

    private MongoClient client;
    private JoystickEventCrudService crud;

    @BeforeEach
    void setUp() throws Exception {
        client = MongoClients.create(MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString("mongodb://127.0.0.1:1"))
                .applyToClusterSettings(builder ->
                        builder.serverSelectionTimeout(SELECTION_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS))
                .applyToSocketSettings(builder ->
                        builder.connectTimeout(SELECTION_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS))
                .build());
        JoystickEventCrudService service = new JoystickEventCrudService();
        Field eventStore = EventCrudService.class.getDeclaredField("eventStore");
        eventStore.setAccessible(true);
        eventStore.set(service, new MongoEventStore(client, "autonomy"));
        crud = service;
    }

    @AfterEach
    void tearDown() {
        if (client != null) {
            client.close();
        }
    }

    @Test
    void hallwayReleaseIsNotStuckBehindMongoSelection() {
        assertTimeoutPreemptively(RELEASE_BUDGET, () -> {
            crud.actOnJoystickEvent(button(11L, 1L, JoystickEventType.BUTTON_DOWN));
            crud.actOnJoystickEvent(button(11L, 0L, JoystickEventType.BUTTON_UP));
        }, "11:0 release waited on Mongo server selection for the button-down persist");
    }

    private static JoystickEvent button(long number, long value, JoystickEventType type) {
        return new JoystickEvent(
                UUID.randomUUID().toString(),
                Instant.parse("2026-10-27T00:00:00Z"),
                GenerationOrigin.NON_HUMAN,
                "JSTestEvent",
                "hallway-joystick",
                number + ":" + value,
                number,
                value,
                false,
                type);
    }
}
