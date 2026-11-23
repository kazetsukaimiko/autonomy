package io.freedriver.autonomy.scheduled;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import io.freedriver.autonomy.mqtt.CacheApplianceStateSource;
import io.freedriver.autonomy.mqtt.MqttStatePublisher;
import jakarta.enterprise.inject.Instance;
import org.junit.jupiter.api.Test;

/**
 * A firing still switches and drops the row when the connector has not started.
 * The acknowledgement is the only step skipped.
 */
class NullConnectorFiringTest {

    @Test
    void aFiringWithANullConnectorSwitchesAndSkipsTheAcknowledgement() throws Exception {
        Path dir = Files.createTempDirectory("autonomy-null-connector");
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000008");
        UUID listId = UUID.fromString("00000000-0000-0000-0000-000000000007");
        Instant due = Instant.now().minusSeconds(5);
        try (H2PendingStore seeding = H2PendingStore.open(dir).orElseThrow()) {
            seeding.admit(listId, Instant.now(), Set.of(), Set.of(),
                    List.of(new PendingRow(id, "lamp", false, due)));
        }

        RecordingGateway gateway = new RecordingGateway();
        AtomicInteger reads = new AtomicInteger();
        MqttStatePublisher publisher = mock(MqttStatePublisher.class);
        when(publisher.connector()).thenAnswer(invocation -> {
            reads.incrementAndGet();
            return null;
        });
        @SuppressWarnings("unchecked")
        Instance<MqttStatePublisher> publishers = mock(Instance.class);
        when(publishers.isResolvable()).thenReturn(true);
        when(publishers.get()).thenReturn(publisher);
        CacheApplianceStateSource states = mock(CacheApplianceStateSource.class);
        when(states.boardFor("lamp")).thenReturn(Optional.of(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")));

        ScheduledCommands commands = new ScheduledCommands(
                dir.toString(),
                "true",
                "classpath:db/scheduled",
                publishers,
                gateway,
                states);
        try {
            commands.onStart(null);
            long deadline = System.nanoTime() + java.time.Duration.ofSeconds(5).toNanos();
            while (gateway.writes.isEmpty() && System.nanoTime() < deadline) {
                Thread.sleep(20);
            }
            assertEquals(List.of(Map.of("lamp", false)), gateway.writes);
            assertTrue(commands.store().find(id).isEmpty());
            assertEquals(1, reads.get());
        } finally {
            commands.onStop(null);
        }
    }

    private static final class RecordingGateway implements CommandListEngine.BoardGateway {
        private final List<Map<String, Boolean>> writes = new ArrayList<>();

        @Override
        public CommandListEngine.WriteResult write(UUID board, Map<String, Boolean> desired) {
            writes.add(Map.copyOf(desired));
            return CommandListEngine.WriteResult.APPLIED;
        }
    }
}
