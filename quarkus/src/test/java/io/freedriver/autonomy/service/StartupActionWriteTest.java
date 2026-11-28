package io.freedriver.autonomy.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.freedriver.autonomy.scheduled.AliasBoardGateway;
import io.freedriver.autonomy.scheduled.CommandListEngine;
import io.freedriver.autonomy.scheduled.PendingRow;
import io.freedriver.autonomy.scheduled.PendingStore;
import io.freedriver.autonomy.service.startup.StartupActionService;
import io.freedriver.jsonlink.Connector;
import io.freedriver.jsonlink.ConnectorException;
import io.freedriver.jsonlink.api.v1.BoardRequest;
import io.freedriver.jsonlink.api.v1.BoardResponse;
import io.freedriver.jsonlink.api.v1.DigitalWrite;
import io.freedriver.jsonlink.api.v1.Identifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * A write whose response was taken before this connection's startup actions
 * finished is failed, so an off stays pending and retries. Missing or contrary
 * feedback after startup has finished is a failed write as well.
 */
class StartupActionWriteTest {

    private static final UUID BOARD = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final Identifier PIN = Identifier.of(4);
    private static final Instant NOW = Instant.parse("2026-11-22T18:00:00Z");

    private Harness harness;

    @AfterEach
    void dropConnector() {
        if (harness != null && harness.connector != null) {
            harness.forgetTrackedConnector(harness.connector);
        }
    }

    @Test
    void missingFeedbackAfterARestartIsAFailedWrite() throws Exception {
        World world = world();
        world.connect();
        world.reconnect();
        harness.connector.feedback = Feedback.MISSING;
        assertEquals(CommandListEngine.WriteResult.FAILED, world.writeOff());
        assertTrue(harness.startupActionsFinished(BOARD));
        assertFalse(harness.lastSendPrecededStartupActions());
    }

    @Test
    void contraryFeedbackAfterARestartIsAFailedWrite() throws Exception {
        World world = world();
        world.connect();
        world.reconnect();
        harness.connector.feedback = Feedback.CONTRARY;
        assertEquals(CommandListEngine.WriteResult.FAILED, world.writeOff());
        assertTrue(harness.startupActionsFinished(BOARD));
        assertFalse(harness.lastSendPrecededStartupActions());
    }

    @Test
    void aConfirmedWriteAfterStartupFinishedIsApplied() throws Exception {
        World world = world();
        world.connect();
        world.reconnect();
        assertEquals(CommandListEngine.WriteResult.APPLIED, world.writeOff());
        assertTrue(harness.startupActionsFinished(BOARD));
        assertFalse(harness.lastSendPrecededStartupActions());
        assertEquals(Boolean.FALSE, harness.connector.pins.get(PIN));
    }

    @Test
    void aStartupActionThatTurnsTheTargetOnKeepsTheOffPending() throws Exception {
        World world = world();
        world.connect();
        harness.connector.pins.put(PIN, false);
        harness.gate.blockNext = true;
        harness.gate.afterRelease = () -> harness.connector.pins.put(PIN, true);

        Thread reconnect = new Thread(world::reconnect, "startup-reconnect");
        reconnect.start();
        assertTrue(harness.gate.entered.await(5, TimeUnit.SECONDS));
        assertFalse(harness.startupActionsFinished(BOARD));

        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000002");
        Instant due = NOW.minusSeconds(5);
        world.store.rows.put(id, new PendingRow(id, "lamp", false, due));
        world.engine.onFire(id, due);

        assertEquals(List.of(Map.of("lamp", false)), world.writes);
        assertEquals(1, world.store.rows.size());
        assertTrue(world.acked.isEmpty());
        assertEquals(1, world.alarms.size());
        assertEquals(NOW.plusSeconds(10), world.alarms.get(0).when());
        harness.gate.release.countDown();
        reconnect.join(5_000);
        assertFalse(reconnect.isAlive());
        assertTrue(harness.startupActionsFinished(BOARD));
        assertEquals(Boolean.TRUE, harness.connector.pins.get(PIN));
        assertEquals(1, world.store.rows.size());
    }

    private World world() throws Exception {
        harness = new Harness();
        SimpleAliasService aliases = mock(SimpleAliasService.class);
        when(aliases.setState(eq(BOARD), any())).thenAnswer(invocation -> {
            Map<String, Boolean> desired = invocation.getArgument(1);
            return harness.send(BOARD, BoardRequest.empty().digitalWrite(new DigitalWrite(PIN, desired.get("lamp"))));
        });
        when(aliases.identifiers(eq(BOARD), any())).thenAnswer(invocation -> {
            Map<String, Boolean> desired = invocation.getArgument(1);
            return Map.of(PIN, desired.get("lamp"));
        });
        AliasBoardGateway gateway = new AliasBoardGateway(aliases, harness);
        MemoryStore store = new MemoryStore();
        List<Map<String, Boolean>> writes = new ArrayList<>();
        List<Alarm> alarms = new ArrayList<>();
        List<String> acked = new ArrayList<>();
        CommandListEngine engine = new CommandListEngine(
                store,
                name -> Optional.of(BOARD),
                (board, desired) -> {
                    CommandListEngine.WriteResult result = gateway.write(board, desired);
                    writes.add(Map.copyOf(desired));
                    return result;
                },
                (id, dueAt, when) -> alarms.add(new Alarm(id, dueAt, when)),
                () -> NOW,
                () -> true,
                id -> acked.add(id.toString()));
        return new World(store, engine, writes, alarms, acked);
    }

    private final class World {
        private final MemoryStore store;
        private final CommandListEngine engine;
        private final List<Map<String, Boolean>> writes;
        private final List<Alarm> alarms;
        private final List<String> acked;

        private World(MemoryStore store, CommandListEngine engine, List<Map<String, Boolean>> writes,
                List<Alarm> alarms, List<String> acked) {
            this.store = store;
            this.engine = engine;
            this.writes = writes;
            this.alarms = alarms;
            this.acked = acked;
        }

        private void connect() {
            harness.getConnectedBoards();
        }

        private void reconnect() {
            harness.connector.closed = true;
            harness.reconnecting = true;
            harness.getConnectedBoards();
        }

        private CommandListEngine.WriteResult writeOff() {
            return new AliasBoardGateway(mockSetState(), harness).write(BOARD, Map.of("lamp", false));
        }

        private SimpleAliasService mockSetState() {
            SimpleAliasService aliases = mock(SimpleAliasService.class);
            try {
                when(aliases.setState(eq(BOARD), any())).thenAnswer(invocation -> {
                    Map<String, Boolean> desired = invocation.getArgument(1);
                    return harness.send(BOARD,
                            BoardRequest.empty().digitalWrite(new DigitalWrite(PIN, desired.get("lamp"))));
                });
                when(aliases.identifiers(eq(BOARD), any())).thenAnswer(invocation -> {
                    Map<String, Boolean> desired = invocation.getArgument(1);
                    return Map.of(PIN, desired.get("lamp"));
                });
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            return aliases;
        }
    }

    private enum Feedback {
        ECHO, MISSING, CONTRARY
    }

    private static final class Gate {
        private boolean blockNext;
        private Runnable afterRelease = () -> {
        };
        private final CountDownLatch entered = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);

        private StartupActionService service() {
            StartupActionService startup = mock(StartupActionService.class);
            doAnswer(invocation -> {
                if (!blockNext) {
                    return null;
                }
                blockNext = false;
                entered.countDown();
                if (!release.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("startup was not released");
                }
                afterRelease.run();
                return null;
            }).when(startup).onBoardEnumerated(any());
            return startup;
        }
    }

    private final class Harness extends ConnectorService {
        private final ScriptedConnector connector = new ScriptedConnector();
        private final Gate gate = new Gate();
        private boolean reconnecting;
        private boolean published;

        private Harness() {
            startupActions = gate.service();
        }

        @Override
        protected void discoverDevices(Collection<UUID> newlyConnected) {
            if (reconnecting) {
                connector.closed = false;
                reconnecting = false;
                published = false;
            }
            if (!published && !connector.closed) {
                publishConnectedBoard(connector, newlyConnected);
                published = true;
            }
        }
    }

    private static final class ScriptedConnector implements Connector {
        private final Map<Identifier, Boolean> pins = new LinkedHashMap<>();
        private volatile boolean closed;
        private volatile Feedback feedback = Feedback.ECHO;
        private Map<Identifier, Boolean> requested = Map.of();

        @Override
        public UUID getUUID() {
            return BOARD;
        }

        @Override
        public UUID makeRequest(BoardRequest request, Duration maxWait) {
            if (request.write() != null && request.write().digital() != null) {
                requested = new LinkedHashMap<>();
                request.write().digital().forEach((pin, state) -> requested.put(pin, state.getValue()));
                if (feedback == Feedback.ECHO) {
                    pins.putAll(requested);
                }
            }
            return UUID.randomUUID();
        }

        @Override
        public BoardResponse getResponse(UUID requestId, Duration maxWait) throws ConnectorException {
            if (feedback == Feedback.MISSING) {
                return new BoardResponse(BOARD, null, null, null, null, null, null, null);
            }
            if (feedback == Feedback.CONTRARY) {
                Map<Identifier, Boolean> lie = new LinkedHashMap<>();
                requested.forEach((pin, value) -> lie.put(pin, !value));
                return new BoardResponse(BOARD, null, null, null, null, null, lie, null);
            }
            return new BoardResponse(BOARD, null, null, null, null, null, new LinkedHashMap<>(pins), null);
        }

        @Override
        public String device() {
            return "/dev/fake-startup";
        }

        @Override
        public boolean isClosed() {
            return closed;
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    private record Alarm(UUID id, Instant dueAt, Instant when) {
    }

    private static final class MemoryStore implements PendingStore {
        private final Map<UUID, PendingRow> rows = new LinkedHashMap<>();

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
            return false;
        }

        @Override
        public void admit(UUID listId, Instant seenAt, Set<UUID> cancelIds, Set<String> cancelTargets,
                List<PendingRow> added) {
            rows.clear();
            for (PendingRow row : added) {
                rows.put(row.id(), row);
            }
        }

        @Override
        public void delete(UUID id) {
            rows.remove(id);
        }

        @Override
        public void purgeSeenBefore(Instant cutoff) {
        }
    }
}
