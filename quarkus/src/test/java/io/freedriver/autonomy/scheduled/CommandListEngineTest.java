package io.freedriver.autonomy.scheduled;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.freedriver.autonomy.mqtt.CommandListResult;
import io.freedriver.mqtt.contract.ApplianceCommandEntry;
import io.freedriver.mqtt.contract.ApplianceCommandList;
import io.freedriver.mqtt.contract.CancelCommandEntry;
import io.freedriver.mqtt.contract.CommandListEntry;
import org.junit.jupiter.api.Test;

class CommandListEngineTest {

    private static final Instant NOW = Instant.parse("2026-11-22T18:00:00Z");
    private static final UUID BOARD_A = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID BOARD_B = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Test
    void undelayedEntriesAreOneWritePerBoard() {
        Harness harness = harness(Map.of("lamp", BOARD_A, "fan", BOARD_B, "porch", BOARD_A));
        CommandListResult result = harness.engine.accept(list(1, NOW, List.of(
                on(2, "lamp", true, null),
                on(3, "fan", true, null),
                on(4, "porch", false, null))), NOW);
        assertInstanceOf(CommandListResult.Applied.class, result);
        assertEquals(2, harness.writes.size());
        assertEquals(BOARD_A, harness.writes.get(0).board());
        assertEquals(Map.of("lamp", true, "porch", false), harness.writes.get(0).desired());
        assertEquals(BOARD_B, harness.writes.get(1).board());
        assertEquals(Map.of("fan", true), harness.writes.get(1).desired());
    }

    @Test
    void aMixedListKeepsRunsOnEitherSideOfASequencePoint() {
        Harness harness = harness(Map.of("lamp", BOARD_A, "fan", BOARD_B, "porch", BOARD_A));
        harness.engine.accept(list(1, NOW, List.of(
                on(2, "lamp", true, null),
                on(3, "fan", true, null),
                cancelTarget(4, "nowhere"),
                on(5, "porch", true, null))), NOW);
        assertEquals(3, harness.writes.size());
        assertEquals(Map.of("lamp", true), harness.writes.get(0).desired());
        assertEquals(Map.of("fan", true), harness.writes.get(1).desired());
        assertEquals(Map.of("porch", true), harness.writes.get(2).desired());
    }

    @Test
    void cancelByTargetAndByIdDropPendingRows() {
        Harness harness = harness(Map.of("lamp", BOARD_A));
        harness.engine.accept(list(1, NOW, List.of(
                on(2, "lamp", false, Duration.ofSeconds(30)),
                on(3, "lamp", true, Duration.ofSeconds(60)))), NOW);
        assertEquals(2, harness.engine.pending().size());
        harness.engine.accept(list(4, NOW.plusSeconds(1), List.of(cancelId(5, id(2)))), NOW.plusSeconds(1));
        assertEquals(1, harness.engine.pending().size());
        harness.engine.accept(list(6, NOW.plusSeconds(2), List.of(cancelTarget(7, "lamp"))), NOW.plusSeconds(2));
        assertTrue(harness.engine.pending().isEmpty());
    }

    @Test
    void overCapacityAndARepeatedIdRefuseTheWholeList() {
        Harness harness = harness(Map.of("lamp", BOARD_A));
        List<CommandListEntry> fill = new ArrayList<>();
        for (int n = 2; n < 130; n++) {
            fill.add(on(n, "lamp", false, Duration.ofSeconds(n)));
        }
        assertInstanceOf(CommandListResult.Applied.class, harness.engine.accept(list(1, NOW, fill), NOW));
        CommandListResult extra = harness.engine.accept(list(200, NOW, List.of(
                on(201, "lamp", false, Duration.ofSeconds(5)))), NOW);
        assertInstanceOf(CommandListResult.Refused.class, extra);
        assertEquals(128, harness.engine.pending().size());
        CommandListResult repeated = harness.engine.accept(list(202, NOW, List.of(
                on(2, "lamp", true, Duration.ofSeconds(9)))), NOW);
        assertInstanceOf(CommandListResult.Refused.class, repeated);
        assertEquals(128, harness.engine.pending().size());
    }

    @Test
    void aCancelFreesASlotForALaterEntryInTheSameList() {
        Harness harness = harness(Map.of("lamp", BOARD_A));
        List<CommandListEntry> fill = new ArrayList<>();
        for (int n = 2; n < 130; n++) {
            fill.add(on(n, "lamp", false, Duration.ofSeconds(n)));
        }
        harness.engine.accept(list(1, NOW, fill), NOW);
        CommandListResult result = harness.engine.accept(list(200, NOW, List.of(
                cancelId(201, id(2)),
                on(202, "lamp", false, Duration.ofSeconds(9)))), NOW);
        assertInstanceOf(CommandListResult.Applied.class, result);
        assertEquals(128, harness.engine.pending().size());
    }

    @Test
    void staleAndReplayedListsAreDropped() {
        Harness harness = harness(Map.of("lamp", BOARD_A));
        assertInstanceOf(CommandListResult.Dropped.class,
                harness.engine.accept(list(1, NOW.minusSeconds(21), List.of(on(2, "lamp", true, null))), NOW));
        assertTrue(harness.writes.isEmpty());
        harness.engine.accept(list(3, NOW, List.of(on(4, "lamp", true, null))), NOW);
        assertInstanceOf(CommandListResult.Dropped.class,
                harness.engine.accept(list(3, NOW.plusSeconds(1), List.of(on(5, "lamp", false, null))),
                        NOW.plusSeconds(1)));
        assertEquals(1, harness.writes.size());
    }

    @Test
    void anUnknownTargetIsSkippedAndWaterOnIsHeld() {
        Harness harness = harness(Map.of("lamp", BOARD_A, "water_tank", BOARD_B, "water_inlet", BOARD_B));
        harness.engine.accept(list(1, NOW, List.of(
                on(2, "missing", true, null),
                on(3, "water_tank", true, null),
                on(4, "water_inlet", false, Duration.ofSeconds(5)),
                on(5, "lamp", true, null))), NOW);
        assertEquals(List.of(new Write(BOARD_A, Map.of("lamp", true))), harness.writes);
        assertEquals("water_inlet", harness.engine.pending().get(0).target());
    }

    @Test
    void overdueOffFiresAtRecoveryAndOverdueOnIsDropped() {
        Harness harness = harness(Map.of("lamp", BOARD_A, "fan", BOARD_A));
        harness.clock.set(NOW.plusSeconds(120));
        harness.store.rows.put(UUID.fromString(id(2)),
                new PendingRow(UUID.fromString(id(2)), "lamp", false, NOW.plusSeconds(5)));
        harness.store.rows.put(UUID.fromString(id(3)),
                new PendingRow(UUID.fromString(id(3)), "fan", true, NOW.plusSeconds(5)));
        harness.engine.recover();
        assertEquals(List.of(new Write(BOARD_A, Map.of("lamp", false))), harness.writes);
        assertTrue(harness.engine.pending().isEmpty());
        assertEquals(List.of(id(2)), harness.acked);
    }

    @Test
    void commandsDisabledLeavesRowsPending() {
        Harness harness = harness(Map.of("lamp", BOARD_A));
        harness.enabled.set(false);
        harness.store.rows.put(UUID.fromString(id(2)),
                new PendingRow(UUID.fromString(id(2)), "lamp", false, NOW.minusSeconds(5)));
        harness.engine.recover();
        assertTrue(harness.writes.isEmpty());
        assertEquals(1, harness.engine.pending().size());
        assertInstanceOf(CommandListResult.Dropped.class,
                harness.engine.accept(list(3, NOW, List.of(on(4, "lamp", true, null))), NOW));
    }

    @Test
    void aCancelWinsOverADueFiringAndAnOldTimerDoesNotTouchTheReplacement() {
        Harness harness = harness(Map.of("lamp", BOARD_A));
        harness.engine.accept(list(1, NOW, List.of(on(2, "lamp", false, Duration.ofSeconds(30)))), NOW);
        harness.engine.accept(list(3, NOW.plusSeconds(1), List.of(cancelId(4, id(2)))), NOW.plusSeconds(1));
        harness.engine.onFire(UUID.fromString(id(2)), NOW.plusSeconds(30));
        assertTrue(harness.writes.isEmpty());
        harness.engine.accept(list(5, NOW.plusSeconds(2), List.of(on(2, "lamp", false, Duration.ofSeconds(90)))),
                NOW.plusSeconds(2));
        harness.engine.onFire(UUID.fromString(id(2)), NOW.plusSeconds(30));
        assertTrue(harness.writes.isEmpty());
        assertEquals(NOW.plusSeconds(92), harness.engine.pending().get(0).dueAt());
    }

    @Test
    void anUnknownTargetAtFireTimeIsDeleted() {
        Harness harness = harness(Map.of("lamp", BOARD_A));
        harness.store.rows.put(UUID.fromString(id(2)),
                new PendingRow(UUID.fromString(id(2)), "gone", false, NOW.minusSeconds(1)));
        harness.engine.onFire(UUID.fromString(id(2)), NOW.minusSeconds(1));
        assertTrue(harness.engine.pending().isEmpty());
        assertTrue(harness.writes.isEmpty());
        assertTrue(harness.acked.isEmpty());
    }

    @Test
    void anOffWriteFailureIsRetriedAndAnOnWriteFailureIsDropped() {
        Harness harness = harness(Map.of("lamp", BOARD_A, "fan", BOARD_A));
        harness.engine.accept(list(1, NOW, List.of(
                on(2, "lamp", false, Duration.ofSeconds(5)),
                on(3, "fan", true, Duration.ofSeconds(5)))), NOW);
        harness.alarms.clear();
        harness.result.set(CommandListEngine.WriteResult.FAILED);
        harness.engine.onFire(UUID.fromString(id(2)), NOW.plusSeconds(5));
        assertEquals(1, harness.alarms.size());
        assertEquals(NOW.plusSeconds(10), harness.alarms.get(0).when());
        assertEquals(2, harness.engine.pending().size());
        harness.result.set(CommandListEngine.WriteResult.APPLIED);
        harness.engine.onFire(UUID.fromString(id(2)), NOW.plusSeconds(5));
        assertEquals(List.of(id(2)), harness.acked);
        harness.result.set(CommandListEngine.WriteResult.FAILED);
        harness.engine.onFire(UUID.fromString(id(3)), NOW.plusSeconds(5));
        assertTrue(harness.engine.pending().isEmpty());
        assertEquals(List.of(id(2)), harness.acked);
    }

    @Test
    void unconfirmedOffStaysPending() {
        Harness harness = harness(Map.of("lamp", BOARD_A));
        harness.engine.accept(list(1, NOW, List.of(on(2, "lamp", false, Duration.ofSeconds(5)))), NOW);
        harness.result.set(CommandListEngine.WriteResult.UNCONFIRMED);
        harness.engine.onFire(UUID.fromString(id(2)), NOW.plusSeconds(5));
        assertEquals(1, harness.engine.pending().size());
        assertTrue(harness.acked.isEmpty());
    }

    @Test
    void aFailedSecondBoardStillArmsTheDelayedRowAndAReplayIsDropped() {
        Harness harness = harness(Map.of("lamp", BOARD_A, "fan", BOARD_B));
        harness.failBoard.set(BOARD_B);
        CommandListResult result = harness.engine.accept(list(1, NOW, List.of(
                on(2, "lamp", true, null),
                on(3, "fan", true, null),
                on(4, "lamp", false, Duration.ofSeconds(15)))), NOW);
        assertInstanceOf(CommandListResult.Dropped.class, result);
        assertEquals(1, harness.engine.pending().size());
        assertEquals(1, harness.alarms.size());
        assertTrue(harness.acked.isEmpty());
        assertInstanceOf(CommandListResult.Dropped.class,
                harness.engine.accept(list(1, NOW.plusSeconds(1), List.of(on(5, "fan", true, null))),
                        NOW.plusSeconds(1)));
    }

    @Test
    void aStoreFailureSendsNothing() {
        Harness harness = harness(Map.of("lamp", BOARD_A));
        harness.store.failAdmit.set(true);
        assertThrows(IllegalStateException.class,
                () -> harness.engine.accept(list(1, NOW, List.of(on(2, "lamp", true, null))), NOW));
        assertTrue(harness.writes.isEmpty());
        assertTrue(harness.engine.pending().isEmpty());
    }

    private static Harness harness(Map<String, UUID> boards) {
        Harness harness = new Harness(boards);
        harness.engine = new CommandListEngine(
                harness.store,
                name -> Optional.ofNullable(boards.get(name)),
                (board, desired) -> {
                    if (board.equals(harness.failBoard.get())) {
                        return CommandListEngine.WriteResult.FAILED;
                    }
                    harness.writes.add(new Write(board, Map.copyOf(desired)));
                    return harness.result.get();
                },
                (id, dueAt, when) -> harness.alarms.add(new Alarm(id, dueAt, when)),
                () -> harness.clock.get(),
                harness.enabled::get,
                id -> harness.acked.add(id.toString()));
        return harness;
    }

    private static String id(int n) {
        return String.format("00000000-0000-0000-0000-%012d", n);
    }

    private static ApplianceCommandList list(int listId, Instant issuedAt, List<CommandListEntry> commands) {
        return new ApplianceCommandList(id(listId), issuedAt, commands);
    }

    private static ApplianceCommandEntry on(int entry, String target, boolean state, Duration delay) {
        return new ApplianceCommandEntry(id(entry), target, state, delay);
    }

    private static CancelCommandEntry cancelId(int entry, String cancelId) {
        return new CancelCommandEntry(id(entry), null, cancelId);
    }

    private static CancelCommandEntry cancelTarget(int entry, String target) {
        return new CancelCommandEntry(id(entry), target, null);
    }

    private static final class Harness {
        private final MemoryStore store = new MemoryStore();
        private final List<Write> writes = new ArrayList<>();
        private final List<Alarm> alarms = new ArrayList<>();
        private final List<String> acked = new ArrayList<>();
        private final AtomicReference<Instant> clock = new AtomicReference<>(NOW);
        private final AtomicBoolean enabled = new AtomicBoolean(true);
        private final AtomicReference<CommandListEngine.WriteResult> result =
                new AtomicReference<>(CommandListEngine.WriteResult.APPLIED);
        private final AtomicReference<UUID> failBoard = new AtomicReference<>();
        private CommandListEngine engine;

        private Harness(Map<String, UUID> boards) {
        }
    }

    private record Write(UUID board, Map<String, Boolean> desired) {
    }

    private record Alarm(UUID id, Instant dueAt, Instant when) {
    }

    private static final class MemoryStore implements PendingStore {
        private final Map<UUID, PendingRow> rows = new LinkedHashMap<>();
        private final Map<UUID, Instant> seen = new LinkedHashMap<>();
        private final AtomicBoolean failAdmit = new AtomicBoolean();

        @Override
        public List<PendingRow> pending() {
            return List.copyOf(rows.values());
        }

        @Override
        public Optional<PendingRow> find(UUID id) {
            return Optional.ofNullable(rows.get(id));
        }

        @Override
        public boolean seen(UUID listId, Instant now) {
            purgeSeenBefore(now.minus(H2PendingStore.SEEN_FOR));
            return seen.containsKey(listId);
        }

        @Override
        public void admit(UUID listId, Instant seenAt, Set<UUID> cancelIds, Set<String> cancelTargets,
                List<PendingRow> added) {
            if (failAdmit.get()) {
                throw new IllegalStateException("store");
            }
            rows.keySet().removeAll(cancelIds);
            rows.values().removeIf(row -> cancelTargets.contains(row.target()));
            for (PendingRow row : added) {
                rows.put(row.id(), row);
            }
            seen.put(listId, seenAt);
        }

        @Override
        public void delete(UUID id) {
            rows.remove(id);
        }

        @Override
        public void purgeSeenBefore(Instant cutoff) {
            seen.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoff));
        }
    }
}
