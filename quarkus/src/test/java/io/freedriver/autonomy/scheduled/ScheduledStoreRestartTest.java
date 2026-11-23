package io.freedriver.autonomy.scheduled;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import io.freedriver.autonomy.MappingsTestResource;
import io.freedriver.autonomy.mqtt.MqttStatePublisher;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

/**
 * A packaged boot opens the H2 file, stops, and opens it again with MQTT off.
 * A future row is still pending. Nothing is published.
 */
@QuarkusTest
@QuarkusTestResource(MappingsTestResource.class)
@TestProfile(ScheduledStoreRestartTest.Profile.class)
class ScheduledStoreRestartTest {

    @Inject
    ScheduledCommands scheduled;

    @Inject
    MqttStatePublisher publisher;

    @AfterAll
    static void cleanup() throws IOException {
        StoreFailureStartupTest.relax(Profile.DIR);
    }

    @Test
    void aFutureRowSurvivesRestartWhileTheBrokerIsDown() {
        assertNull(publisher.connector());
        assertTrue(scheduled.available());
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000042");
        Instant due = Instant.ofEpochMilli(System.currentTimeMillis() + 3_600_000L);
        scheduled.store().admit(
                UUID.fromString("00000000-0000-0000-0000-000000000041"),
                Instant.ofEpochMilli(System.currentTimeMillis()),
                Set.of(),
                Set.of(),
                List.of(new PendingRow(id, "lamp", false, due)));
        scheduled.onStop(null);
        scheduled.onStart(null);
        assertTrue(scheduled.available());
        assertNull(publisher.connector());
        assertEquals(List.of(new PendingRow(id, "lamp", false, due)), scheduled.store().pending());
    }

    public static class Profile implements QuarkusTestProfile {
        static final Path DIR = directory();

        private static Path directory() {
            try {
                return StoreFailureStartupTest.freshDirectory("autonomy-scheduled-restart");
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("autonomy.scheduled.store-dir", DIR.toString());
        }
    }
}
