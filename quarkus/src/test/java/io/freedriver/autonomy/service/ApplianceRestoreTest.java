package io.freedriver.autonomy.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import io.freedriver.autonomy.event.GenerationOrigin;
import io.freedriver.autonomy.event.input.joystick.JoystickEvent;
import io.freedriver.autonomy.event.input.joystick.JoystickEventType;
import io.freedriver.jsonlink.Connector;
import io.freedriver.jsonlink.ConnectorException;
import io.freedriver.jsonlink.config.v2.Appliance;
import io.freedriver.jsonlink.config.v2.Mapping;
import io.freedriver.jsonlink.config.v2.Mappings;
import io.freedriver.jsonlink.jackson.schema.v1.Identifier;
import io.freedriver.jsonlink.jackson.schema.v1.Request;
import io.freedriver.jsonlink.jackson.schema.v1.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Appliance restore against an in-memory board. The board powers up with every output off.
 */
class ApplianceRestoreTest {

    private static final Identifier FRIDGE_PIN = Identifier.of(40);
    private static final Identifier HALLWAY_PIN = Identifier.of(41);

    @TempDir
    Path temp;

    private final UUID boardId = UUID.randomUUID();
    private final MutableClock clock = new MutableClock(Instant.parse("2026-09-27T12:00:00Z"));
    private final Mapping mapping = new Mapping(
            boardId,
            "main",
            List.of(new Appliance(FRIDGE_PIN, "fridge"), new Appliance(HALLWAY_PIN, "hallway")),
            Map.of("11:0", List.of("fridge")),
            Set.of(),
            List.of());

    private ApplianceStateStore store;
    private FakeBoard board;
    private FixedMappingAliasService aliases;
    private ApplianceRestoreService restore;
    private ConnectorServiceCommon connectors;
    private Connector connector;

    @BeforeEach
    void setUp() {
        store = new ApplianceStateStore(temp.resolve("state"));
        board = new FakeBoard();
        aliases = new FixedMappingAliasService(mapping);
        aliases.connectorService = board;
        aliases.digitalPinCache = new ConcurrentHashMap<>();
        aliases.applianceStateStore = store;
        restore = new ApplianceRestoreService(store, clock, Duration.ofSeconds(30));
        restore.aliases = aliases;
        aliases.applianceRestoreService = restore;
        connectors = new ConnectorServiceCommon();
        connectors.applianceRestoreService = restore;
        connector = new NamedConnector(boardId);
    }

    @AfterEach
    void tearDown() {
        if (connector != null) {
            connectors.forgetTrackedConnector(connector);
        }
        board.executorService.shutdownNow();
    }

    @Test
    void savesARestSetStateChangeAfterTheBoardConfirmsIt() throws IOException {
        aliases.setState(boardId, Map.of("fridge", true));

        assertEquals(Boolean.TRUE, board.pins.get(FRIDGE_PIN));
        assertEquals(Boolean.TRUE, store.load().states().get("fridge"));
        assertFalse(store.load().states().containsKey("hallway"));
    }

    @Test
    void doesNotSaveASetStateChangeTheBoardRejects() {
        board.failNextSend = true;

        assertThrows(IllegalStateException.class, () -> aliases.setState(boardId, Map.of("fridge", true)));
        assertEquals(LoadedApplianceState.Status.MISSING, store.load().status());
        assertTrue(board.pins.isEmpty());
    }

    @Test
    void savesAJoystickChange() throws IOException {
        aliases.digitalPinCache.put(new PinCoordinate(boardId, FRIDGE_PIN), false);
        aliases.digitalPinCache.put(new PinCoordinate(boardId, HALLWAY_PIN), false);
        JoystickEvent press = new JoystickEvent(
                "event-1",
                clock.instant(),
                GenerationOrigin.HUMAN,
                "test",
                "joystick",
                "11:0",
                11L,
                0L,
                false,
                JoystickEventType.BUTTON_UP);

        aliases.handleJoystickEvent(press);

        assertEquals(Boolean.TRUE, board.pins.get(FRIDGE_PIN));
        assertEquals(Boolean.TRUE, store.load().states().get("fridge"));
    }

    @Test
    void savesABoardReportedOutputChange() throws IOException {
        aliases.cacheBoardState(mapping, digital(Map.of(HALLWAY_PIN, true)));
        aliases.cacheBoardDigitalState(boardId, Map.of(FRIDGE_PIN, true));

        assertEquals(Boolean.TRUE, store.load().states().get("hallway"));
        assertEquals(Boolean.TRUE, store.load().states().get("fridge"));
    }

    @Test
    void restoreTurnsSavedOnAppliancesBackOnAndLeavesOffAppliancesOff() throws IOException {
        store.write(Map.of("fridge", true, "hallway", false));
        List<UUID> newlyConnected = new ArrayList<>();
        connectors.publishConnectedBoard(connector, newlyConnected);

        List<String> lines = capture(() -> {
            aliases.cacheBoardState(mapping, digital(Map.of(FRIDGE_PIN, false, HALLWAY_PIN, false)));
            assertEquals(Boolean.TRUE, store.load().states().get("fridge"));
            connectors.restoreNewlyConnected(newlyConnected);
        });

        assertEquals(Boolean.TRUE, board.pins.get(FRIDGE_PIN));
        assertEquals(Boolean.FALSE, board.pins.get(HALLWAY_PIN));
        assertEquals(Boolean.TRUE, store.load().states().get("fridge"));
        assertEquals(Boolean.FALSE, store.load().states().get("hallway"));
        assertEquals(1, lines.stream().filter(line -> line.equals("Restored appliance fridge to ON")).count(), lines::toString);
        assertEquals(1, lines.stream().filter(line -> line.equals("Restored appliance hallway to OFF")).count(), lines::toString);
        assertFalse(restore.isAwaitingRestore(boardId));
    }

    @Test
    void reconnectRestoresAgainOnlyOutsideTheWindow() throws IOException {
        store.write(Map.of("fridge", true, "hallway", false));
        restore.beginAwaitingRestore(boardId);
        restore.onBoardsReady(List.of(boardId));
        assertEquals(1, writeCount());

        board.pins.clear();
        board.requests.clear();
        restore.beginAwaitingRestore(boardId);
        List<String> insideWindow = capture(() -> {
            aliases.cacheBoardState(mapping, digital(Map.of(FRIDGE_PIN, false, HALLWAY_PIN, false)));
            restore.onBoardsReady(List.of(boardId));
        });

        assertEquals(0, writeCount());
        assertEquals(Boolean.TRUE, store.load().states().get("fridge"));
        assertTrue(
                insideWindow.stream().anyMatch(line -> line.contains("not restoring again")),
                insideWindow::toString);
        assertTrue(insideWindow.stream().noneMatch(line -> line.startsWith("Restored appliance ")));
        assertTrue(restore.isAwaitingRestore(boardId));

        clock.advance(Duration.ofSeconds(30));
        board.pins.clear();
        restore.beginAwaitingRestore(boardId);
        List<String> afterWindow = capture(() -> restore.onBoardsReady(List.of(boardId)));

        assertEquals(1, writeCount());
        assertEquals(Boolean.TRUE, board.pins.get(FRIDGE_PIN));
        assertEquals(Boolean.FALSE, board.pins.get(HALLWAY_PIN));
        assertEquals(1, afterWindow.stream().filter(line -> line.equals("Restored appliance fridge to ON")).count());
        assertFalse(restore.isAwaitingRestore(boardId));
    }

    @Test
    void firstStartWithNoFileLeavesEveryApplianceOff() {
        List<String> lines = capture(() -> {
            restore.beginAwaitingRestore(boardId);
            restore.onBoardsReady(List.of(boardId));
        });

        assertEquals(0, writeCount());
        assertTrue(board.pins.isEmpty());
        assertEquals(LoadedApplianceState.Status.MISSING, store.load().status());
        assertFalse(Files.exists(store.stateFile()));
        assertTrue(
                lines.stream().anyMatch(line -> line.equals("No saved appliance state exists; leaving every appliance off")),
                lines::toString);
        assertTrue(lines.stream().noneMatch(line -> line.startsWith("Restored appliance ")));
        assertFalse(restore.isAwaitingRestore(boardId));
    }

    @Test
    void badAndUnknownEntriesAreSkippedAndThatApplianceStaysOff() throws IOException {
        Path directory = temp.resolve("state");
        Files.createDirectories(directory);
        Files.writeString(directory.resolve(ApplianceStateStore.FILE_NAME), """
                {"fridge":true,"hallway":"sometimes","ghost":true}
                """);

        List<String> lines = capture(() -> {
            restore.beginAwaitingRestore(boardId);
            restore.onBoardsReady(List.of(boardId));
        });

        assertEquals(Boolean.TRUE, board.pins.get(FRIDGE_PIN));
        assertEquals(Boolean.FALSE, board.pins.get(HALLWAY_PIN));
        assertEquals(Boolean.TRUE, store.load().states().get("fridge"));
        assertEquals(Boolean.FALSE, store.load().states().get("hallway"));
        assertTrue(lines.stream().anyMatch(line -> line.contains("Skipping appliance state entry 'hallway'")), lines::toString);
        assertTrue(lines.stream().anyMatch(line -> line.contains("Skipping saved state for unknown appliance 'ghost'")), lines::toString);
        assertTrue(lines.stream().noneMatch(line -> line.contains("ghost") && line.startsWith("Restored appliance ")));
        assertEquals(1, lines.stream().filter(line -> line.equals("Restored appliance fridge to ON")).count());
        assertEquals(1, lines.stream().filter(line -> line.equals("Restored appliance hallway to OFF")).count());
    }

    @Test
    void corruptFileDoesNotCrashAndRestoresNothing() throws IOException {
        Path directory = temp.resolve("state");
        Files.createDirectories(directory);
        Files.writeString(directory.resolve(ApplianceStateStore.FILE_NAME), "{");

        List<String> lines = capture(() -> {
            restore.beginAwaitingRestore(boardId);
            restore.onBoardsReady(List.of(boardId));
        });

        assertEquals(0, writeCount());
        assertTrue(board.pins.isEmpty());
        assertEquals("{", Files.readString(store.stateFile()));
        assertTrue(lines.stream().anyMatch(line -> line.contains("treating it as no saved state")), lines::toString);
        assertTrue(
                lines.stream().anyMatch(line -> line.equals("No saved appliance state exists; leaving every appliance off")),
                lines::toString);
    }

    private long writeCount() {
        return board.requests.stream()
                .filter(request -> request.write() != null && !request.write().isEmpty())
                .count();
    }

    private static Response digital(Map<Identifier, Boolean> pins) {
        return Response.builder().digital(pins).build();
    }

    private static List<String> capture(Runnable action) {
        Logger restoreLog = Logger.getLogger(ApplianceRestoreService.class.getName());
        Logger storeLog = Logger.getLogger(ApplianceStateStore.class.getName());
        Level restoreLevel = restoreLog.getLevel();
        Level storeLevel = storeLog.getLevel();
        restoreLog.setLevel(Level.ALL);
        storeLog.setLevel(Level.ALL);
        List<String> lines = new ArrayList<>();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                if (record != null && record.getMessage() != null) {
                    lines.add(LogMessages.format(record));
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        handler.setLevel(Level.ALL);
        restoreLog.addHandler(handler);
        storeLog.addHandler(handler);
        try {
            action.run();
        } finally {
            restoreLog.removeHandler(handler);
            storeLog.removeHandler(handler);
            restoreLog.setLevel(restoreLevel);
            storeLog.setLevel(storeLevel);
        }
        return lines;
    }

    private static final class FixedMappingAliasService extends SimpleAliasService {
        private final Mapping mapping;

        private FixedMappingAliasService(Mapping mapping) {
            this.mapping = mapping;
        }

        @Override
        public Mappings getMappings() {
            return new Mappings(null, null, Set.of(mapping));
        }

        @Override
        public Mapping getMapping(UUID boardId) {
            if (!boardId.equals(mapping.connectorId())) {
                throw new IllegalArgumentException("unknown board " + boardId);
            }
            return mapping;
        }
    }

    private static final class FakeBoard extends ConnectorService {
        private final Map<Identifier, Boolean> pins = new LinkedHashMap<>();
        private final List<Request> requests = new ArrayList<>();
        private boolean failNextSend;

        @Override
        public synchronized Response send(UUID uuid, Request request) {
            if (failNextSend) {
                failNextSend = false;
                throw new IllegalStateException("board rejected the request");
            }
            requests.add(request);
            if (request.write() != null) {
                request.write().digital().forEach((pin, state) -> pins.put(pin, state.getValue()));
            }
            return Response.builder().uuid(uuid).digital(Map.copyOf(pins)).build();
        }
    }

    private static final class NamedConnector implements Connector {
        private final UUID id;

        private NamedConnector(UUID id) {
            this.id = id;
        }

        @Override
        public UUID getUUID() {
            return id;
        }

        @Override
        public UUID makeRequest(Request request, Duration maxWait) {
            return UUID.randomUUID();
        }

        @Override
        public Response getResponse(UUID requestId, Duration maxWait) throws ConnectorException {
            return Response.builder().build();
        }

        @Override
        public String device() {
            return "/dev/fake-board";
        }

        @Override
        public boolean isClosed() {
            return false;
        }

        @Override
        public void close() {
        }
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        private MutableClock(Instant now) {
            this.now = now;
        }

        private void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
