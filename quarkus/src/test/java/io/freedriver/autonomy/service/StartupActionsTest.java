package io.freedriver.autonomy.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

import io.freedriver.autonomy.TestMappings;
import io.freedriver.autonomy.event.GenerationOrigin;
import io.freedriver.autonomy.event.input.joystick.JoystickEvent;
import io.freedriver.autonomy.event.input.joystick.JoystickEventType;
import io.freedriver.autonomy.event.speech.SpeechEvent;
import io.freedriver.autonomy.event.speech.SpeechEventType;
import io.freedriver.autonomy.service.startup.AutonomyMappingsAction;
import io.freedriver.autonomy.service.startup.DelayAction;
import io.freedriver.autonomy.service.startup.GpioAction;
import io.freedriver.autonomy.service.startup.StartupActionRunner;
import io.freedriver.autonomy.service.startup.StartupActionService;
import io.freedriver.autonomy.service.startup.StartupMappingsLoader;
import io.freedriver.jsonlink.Connector;
import io.freedriver.jsonlink.ConnectorTimeoutException;
import io.freedriver.jsonlink.config.v2.MappingsAction;
import io.freedriver.jsonlink.jackson.schema.v1.Identifier;
import io.freedriver.jsonlink.jackson.schema.v1.Request;
import io.freedriver.jsonlink.jackson.schema.v1.Response;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class StartupActionsTest {
    private static final Identifier FRIDGE = Identifier.of(22);
    private static final Identifier HALLWAY = Identifier.of(23);
    private static final Identifier PUMP = Identifier.of(24);
    private static final Identifier OVEN = Identifier.of(25);
    private static final String APPLIANCES = """
            {"identifier":22,"name":"fridge"},
            {"identifier":23,"name":"hallway"},
            {"identifier":24,"name":"water_pump"},
            {"identifier":25,"name":"oven"}""";

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    private final List<Path> trash = new ArrayList<>();
    private final List<Harness> harnesses = new ArrayList<>();
    private final RecordingSpeech speech = new RecordingSpeech();

    @BeforeAll
    static void openValidator() {
        TestMappings.rememberDefaultFingerprint();
        validatorFactory = Validation.byDefaultProvider()
                .configure()
                .messageInterpolator(new ParameterMessageInterpolator())
                .buildValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        if (validatorFactory != null) {
            validatorFactory.close();
        }
        TestMappings.assertDefaultUntouched();
    }

    @AfterEach
    void cleanup() throws IOException {
        for (Harness boardHarness : harnesses) {
            boardHarness.forgetTrackedConnector(boardHarness.connector);
            boardHarness.executorService.shutdownNow();
        }
        for (int i = trash.size() - 1; i >= 0; i--) {
            deleteTree(trash.get(i));
        }
    }

    @Test
    void gpioAndDelayAreRegisteredStartupActions() {
        Map<String, Class<? extends MappingsAction>> types = MappingsAction.registeredTypes();
        assertEquals(GpioAction.class, types.get("gpio"));
        assertEquals(DelayAction.class, types.get("delay"));
        assertTrue(MappingsAction.class.isAssignableFrom(AutonomyMappingsAction.class));
        assertTrue(BiConsumer.class.isAssignableFrom(AutonomyMappingsAction.class));
        assertTrue(AutonomyMappingsAction.class.isAssignableFrom(GpioAction.class));
        assertTrue(AutonomyMappingsAction.class.isAssignableFrom(DelayAction.class));
        assertEquals("gpio", MappingsAction.typeName(GpioAction.class));
        assertEquals("delay", MappingsAction.typeName(DelayAction.class));
    }

    @Test
    void startupActionsRunInOrderUnderTheBoardLock() throws Exception {
        World world = world(gpio("fridge", true) + "," + gpio("hallway", false));
        world.run();
        assertEquals(List.of(FRIDGE.pin(), HALLWAY.pin()), world.harness.writes);
        assertTrue(world.harness.holdCounts.stream().allMatch(holds -> holds >= 2), world.harness.holdCounts::toString);
        assertEquals(Boolean.TRUE, world.harness.pins.get(FRIDGE));
        assertEquals(Boolean.FALSE, world.harness.pins.get(HALLWAY));
        assertTrue(speech.fired.isEmpty());
    }

    @Test
    void reconnectReplaysTheListFromTheTop() throws Exception {
        World world = world(gpio("fridge", true) + "," + gpio("hallway", false));
        world.run();
        world.harness.pins.put(FRIDGE, false);
        world.harness.pins.put(HALLWAY, true);
        world.harness.writes.clear();

        world.harness.connector.closed = true;
        world.harness.reconnecting = true;
        world.harness.refreshConnectedBoards();

        assertEquals(List.of(FRIDGE.pin(), HALLWAY.pin()), world.harness.writes);
        assertEquals(Boolean.TRUE, world.harness.pins.get(FRIDGE));
        assertEquals(Boolean.FALSE, world.harness.pins.get(HALLWAY));
    }

    @Test
    void replayingTheListTwiceLeavesTheSameState() throws Exception {
        World world = world(gpio("fridge", true) + "," + gpio("hallway", false));
        world.harness.pins.put(FRIDGE, false);
        world.harness.pins.put(HALLWAY, true);
        world.harness.pins.put(OVEN, true);
        world.run();
        Map<Identifier, Boolean> afterFirst = Map.copyOf(world.harness.pins);
        List<Integer> firstWrites = List.copyOf(world.harness.writes);
        world.run();
        assertEquals(afterFirst, world.harness.pins);
        assertEquals(firstWrites, world.harness.writes.subList(firstWrites.size(), world.harness.writes.size()));
        assertEquals(Boolean.TRUE, world.harness.pins.get(OVEN));
        assertFalse(world.harness.writes.contains(OVEN.pin()));
    }

    @Test
    void listDoesNotWriteAppliancesThatAreNotNamed() throws Exception {
        World world = world(gpio("fridge", true));
        world.harness.pins.put(OVEN, true);
        world.harness.pins.put(HALLWAY, false);
        world.run();
        assertEquals(List.of(FRIDGE.pin()), world.harness.writes);
        assertEquals(Boolean.TRUE, world.harness.pins.get(OVEN));
        assertEquals(Boolean.FALSE, world.harness.pins.get(HALLWAY));
    }

    @Test
    void boardThatFinishesEnumeratingAfterAnInterruptedHandshakeRunsOnce() throws Exception {
        World world = world(gpio("fridge", true));
        world.harness.forgetTrackedConnector(world.harness.connector);
        world.harness.published = false;
        world.harness.handshakeInterrupted = true;
        world.harness.refreshConnectedBoards();
        assertTrue(world.harness.writes.isEmpty());

        world.harness.handshakeInterrupted = false;
        world.harness.refreshConnectedBoards();
        assertEquals(List.of(FRIDGE.pin()), world.harness.writes);

        world.harness.refreshConnectedBoards();
        assertEquals(List.of(FRIDGE.pin()), world.harness.writes);
    }

    @Test
    void firstFailureSkipsTheRemainingActionsAndNotifies() throws Exception {
        World world = world(gpio("fridge", true) + "," + gpio("hallway", true));
        world.harness.failNext = true;
        world.run();
        assertTrue(world.harness.writes.isEmpty());
        assertFalse(world.harness.pins.containsKey(HALLWAY));
        assertEquals(1, speech.fired.size());
        assertEquals(SpeechEventType.WARNING, speech.fired.get(0).speechEventType());
        assertTrue(speech.fired.get(0).text().contains("rejected"), speech.fired.get(0).text());
    }

    @Test
    void timedOutSendDoesNotUpdateThePinCache() throws Exception {
        World world = world(gpio("fridge", true));
        world.aliases.digitalPinCache.put(new PinCoordinate(world.board, FRIDGE), false);
        world.harness.timeoutAfterApply = true;
        assertThrows(ConnectorTimeoutException.class, () -> world.aliases.setState(world.board, Map.of("fridge", true)));
        assertEquals(Boolean.FALSE, world.aliases.digitalPinCache.get(new PinCoordinate(world.board, FRIDGE)));
        assertEquals(Boolean.TRUE, world.harness.pins.get(FRIDGE));
    }

    @Test
    void timedOutStartupActionDoesNotCacheThePinAndSkipsTheRest() throws Exception {
        World world = world(gpio("fridge", true) + "," + gpio("hallway", true));
        UUID other = UUID.randomUUID();
        world.aliases.digitalPinCache.put(new PinCoordinate(world.board, FRIDGE), false);
        world.aliases.digitalPinCache.put(new PinCoordinate(world.board, HALLWAY), false);
        world.aliases.digitalPinCache.put(new PinCoordinate(other, OVEN), true);
        world.harness.timeoutAfterApply = true;
        world.run();
        assertEquals(Boolean.TRUE, world.harness.pins.get(FRIDGE));
        assertFalse(world.aliases.digitalPinCache.containsKey(new PinCoordinate(world.board, FRIDGE)));
        assertFalse(world.aliases.digitalPinCache.containsKey(new PinCoordinate(world.board, HALLWAY)));
        assertEquals(Boolean.TRUE, world.aliases.digitalPinCache.get(new PinCoordinate(other, OVEN)));
        assertFalse(world.harness.writes.contains(HALLWAY.pin()));
        assertEquals(1, speech.fired.size());
        assertTrue(speech.fired.get(0).text().contains("Timeout"), speech.fired.get(0).text());
    }

    @Test
    void reconnectClearsThatBoardsPinCache() throws Exception {
        World world = world("");
        UUID other = UUID.randomUUID();
        world.aliases.digitalPinCache.put(new PinCoordinate(world.board, FRIDGE), true);
        world.aliases.digitalPinCache.put(new PinCoordinate(world.board, HALLWAY), true);
        world.aliases.digitalPinCache.put(new PinCoordinate(other, OVEN), true);
        world.run();
        assertFalse(world.aliases.digitalPinCache.containsKey(new PinCoordinate(world.board, FRIDGE)));
        assertFalse(world.aliases.digitalPinCache.containsKey(new PinCoordinate(world.board, HALLWAY)));
        assertEquals(Boolean.TRUE, world.aliases.digitalPinCache.get(new PinCoordinate(other, OVEN)));
        assertTrue(world.harness.writes.isEmpty());
    }

    @Test
    void delayReleasesTheBoardLockBetweenActions() throws Exception {
        Gate gate = new Gate();
        World world = world(gpio("fridge", true) + "," + delay("PT1S") + "," + gpio("water_pump", true), gate);
        gate.watch(world.harness);
        Thread startup = new Thread(world::run, "startup-actions");
        startup.start();
        assertTrue(gate.entered.await(5, TimeUnit.SECONDS));
        assertFalse(gate.lockHeld);
        assertEquals(List.of(FRIDGE.pin()), world.harness.writes);
        assertTrue(world.harness.holdCounts.stream().allMatch(holds -> holds >= 2));
        gate.release.countDown();
        startup.join(5000);
        assertFalse(startup.isAlive());
        assertEquals(List.of(FRIDGE.pin(), PUMP.pin()), world.harness.writes);
    }

    @Test
    void restSetStateRespondsWhileADelayRuns() throws Exception {
        Gate gate = new Gate();
        World world = world(gpio("fridge", true) + "," + delay("PT1S") + "," + gpio("water_pump", true), gate);
        duringDelay(world, gate, () -> world.aliases.setState(world.board, Map.of("hallway", true)));
        assertEquals(Boolean.TRUE, world.harness.pins.get(HALLWAY));
        assertTrue(world.harness.holdCounts.contains(1), world.harness.holdCounts::toString);
        assertEquals(List.of(FRIDGE.pin(), HALLWAY.pin(), PUMP.pin()), world.harness.writes);
    }

    @Test
    void joystickRespondsWhileADelayRuns() throws Exception {
        Gate gate = new Gate();
        World world = world(
                gpio("fridge", true) + "," + delay("PT1S") + "," + gpio("water_pump", true),
                gate,
                "\"1:1\":[\"hallway\"]");
        world.harness.pins.put(HALLWAY, false);
        duringDelay(world, gate, () -> {
            world.aliases.handleJoystickEvent(new JoystickEvent(
                    "joy",
                    Instant.now(),
                    GenerationOrigin.NON_HUMAN,
                    "test",
                    "stick",
                    null,
                    1L,
                    1L,
                    false,
                    JoystickEventType.BUTTON_DOWN));
            return null;
        });
        assertEquals(Boolean.TRUE, world.harness.pins.get(HALLWAY));
        assertTrue(world.harness.writes.contains(HALLWAY.pin()));
        assertEquals(PUMP.pin(), world.harness.writes.get(world.harness.writes.size() - 1));
    }

    @Test
    void disconnectDuringADelayStopsTheListAndReconnectRunsFromTheTop() throws Exception {
        Gate gate = new Gate();
        World world = world(gpio("fridge", true) + "," + delay("PT1S") + "," + gpio("water_pump", true), gate);
        Thread startup = new Thread(world::run, "startup-actions");
        startup.start();
        assertTrue(gate.entered.await(5, TimeUnit.SECONDS));
        world.harness.connector.closed = true;
        gate.release.countDown();
        startup.join(5000);
        assertFalse(startup.isAlive());
        assertEquals(List.of(FRIDGE.pin()), world.harness.writes);
        assertTrue(speech.fired.isEmpty(), speech.fired::toString);

        world.harness.reconnecting = true;
        world.harness.refreshConnectedBoards();
        assertEquals(List.of(FRIDGE.pin(), FRIDGE.pin(), PUMP.pin()), world.harness.writes);
    }

    @Test
    void unknownActionTypeRefusesTheListAndRunsNothing() throws Exception {
        World world = world(gpio("fridge", true) + ",{\"type\":\"toggle\",\"appliance\":\"fridge\",\"on\":false}");
        world.run();
        assertTrue(world.harness.writes.isEmpty());
        assertTrue(world.loader.last().fileRefused());
        assertTrue(world.loader.last().errors().get(0).contains("toggle"), world.loader.last().errors()::toString);
        assertTrue(speech.fired.isEmpty());
    }

    @Test
    void delayOfZeroRefusesTheWholeListAndRunsNothing() throws Exception {
        assertDelayRefused("PT0S");
    }

    @Test
    void negativeDelayRefusesTheWholeListAndRunsNothing() throws Exception {
        assertDelayRefused("-PT1S");
    }

    @Test
    void delayOverTenSecondsRefusesTheWholeListAndRunsNothing() throws Exception {
        assertDelayRefused("PT11S");
        assertDelayRefused("PT10.000000001S");
    }

    @Test
    void tenSecondDelayIsWithinTheLimit() throws Exception {
        assertDelayAccepted("PT10S", Duration.ofSeconds(10));
    }

    @Test
    void oneNanosecondDelayIsWithinTheLimit() throws Exception {
        assertDelayAccepted("PT0.000000001S", Duration.ofNanos(1));
    }

    @Test
    void invalidDelayRefusesOnlyThatBoardsList() throws Exception {
        UUID bad = UUID.randomUUID();
        UUID good = UUID.randomUUID();
        String json = """
                {
                  "eventTTL": 7,
                  "eventTTLUnit": "DAYS",
                  "mappings": [
                    {"connectorId":"%s","appliances":[{"identifier":22,"name":"fridge"}]},
                    {"connectorId":"%s","appliances":[{"identifier":24,"name":"water_pump"}]}
                  ],
                  "startupActions": {
                    "%s": [%s, %s, %s],
                    "%s": [%s]
                  }
                }
                """.formatted(bad, good, bad, gpio("fridge", true), delay("PT0S"), gpio("hallway", true), good, gpio("water_pump", true));
        World world = worldFrom(json, bad, new Gate());
        world.run();
        assertTrue(world.harness.writes.isEmpty());
        assertFalse(world.loader.last().fileRefused());
        assertFalse(world.loader.last().actions().containsKey(bad.toString()));
        assertTrue(world.loader.last().actions().containsKey(good.toString()));
        assertTrue(world.loader.last().errors().stream().anyMatch(error -> error.contains(bad.toString())),
                world.loader.last().errors()::toString);

        world.harness.forgetTrackedConnector(world.harness.connector);
        world.harness.connector = new FakeConnector(good);
        world.harness.published = false;
        world.harness.publishConnectedBoard(world.harness.connector, new ArrayList<>());
        world.harness.published = true;
        world.service.onBoardEnumerated(good);
        assertEquals(List.of(PUMP.pin()), world.harness.writes);
    }

    @Test
    void symlinkMappingsFileRunsNothing() throws Exception {
        World world = world(gpio("fridge", true));
        Path link = world.file.getParent().resolve("mappings-link.json");
        Files.createSymbolicLink(link, world.file.getFileName());
        trash.add(link);
        World viaLink = worldFrom(link, world.board);
        viaLink.run();
        assertTrue(viaLink.harness.writes.isEmpty());
        assertTrue(viaLink.loader.last().errors().get(0).contains("symlink"), viaLink.loader.last().errors()::toString);
        world.run();
        assertEquals(List.of(FRIDGE.pin()), world.harness.writes);
    }

    @Test
    void symlinkMappingsDirectoryRunsNothing() throws Exception {
        World world = world(gpio("fridge", true));
        Path parent = Files.createTempDirectory("startup-link-parent");
        ownerOnlyDirectory(parent);
        trash.add(parent);
        Path link = parent.resolve("config");
        Files.createSymbolicLink(link, world.file.getParent());
        World viaLink = worldFrom(link.resolve("mappings_v2.json"), world.board);
        viaLink.run();
        assertTrue(viaLink.harness.writes.isEmpty());
        assertTrue(viaLink.loader.last().errors().stream().anyMatch(error -> error.contains("mappings directory") && error.contains("symlink")),
                viaLink.loader.last().errors()::toString);
    }

    @Test
    void wrongOwnerRunsNothing() throws Exception {
        World world = world(gpio("fridge", true));
        MismatchUidLoader loader = new MismatchUidLoader(validator, world.file);
        StartupActionService service = new StartupActionService(world.harness, world.aliases, speech, loader, new Gate());
        world.harness.startupActions = service;
        service.onBoardEnumerated(world.board);
        assertTrue(world.harness.writes.isEmpty());
        String error = loader.last().errors().get(0);
        assertTrue(error.contains("owner uid"), error);
        assertTrue(error.contains("mappings file"), error);
        assertTrue(error.contains("mappings directory"), error);
        world.run();
        assertEquals(List.of(FRIDGE.pin()), world.harness.writes);
    }

    @Test
    void groupWritableMappingsFileRunsNothing() throws Exception {
        assertPermissionRefused(true, PosixFilePermission.GROUP_WRITE, "mappings file", "writable by group");
    }

    @Test
    void otherWritableMappingsFileRunsNothing() throws Exception {
        assertPermissionRefused(true, PosixFilePermission.OTHERS_WRITE, "mappings file", "writable by other");
    }

    @Test
    void groupWritableMappingsDirectoryRunsNothing() throws Exception {
        assertPermissionRefused(false, PosixFilePermission.GROUP_WRITE, "mappings directory", "writable by group");
    }

    @Test
    void otherWritableMappingsDirectoryRunsNothing() throws Exception {
        assertPermissionRefused(false, PosixFilePermission.OTHERS_WRITE, "mappings directory", "writable by other");
    }

    @Test
    void pollingWaitStopsWhenTheBoardDisconnects() throws Exception {
        AtomicBoolean connected = new AtomicBoolean(true);
        AtomicBoolean stayed = new AtomicBoolean(true);
        Thread waiting = new Thread(() -> {
            try {
                stayed.set(StartupActionRunner.poll(Duration.ofSeconds(5), connected::get));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        waiting.start();
        connected.set(false);
        waiting.join(1000);
        assertFalse(waiting.isAlive());
        assertFalse(stayed.get());
    }

    private void duringDelay(World world, Gate gate, Callable<?> rest) throws Exception {
        gate.watch(world.harness);
        Thread startup = new Thread(world::run, "startup-actions");
        startup.start();
        assertTrue(gate.entered.await(5, TimeUnit.SECONDS));
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<?> pending = pool.submit(rest);
            pending.get(2, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }
        assertEquals(1, gate.release.getCount());
        gate.release.countDown();
        startup.join(5000);
        assertFalse(startup.isAlive());
    }

    private void assertDelayRefused(String iso) throws Exception {
        World world = world(gpio("fridge", true) + "," + delay(iso) + "," + gpio("hallway", true));
        world.run();
        assertTrue(world.harness.writes.isEmpty(), iso);
        assertFalse(world.loader.last().fileRefused(), world.loader.last().errors()::toString);
        assertTrue(world.loader.last().errors().stream().anyMatch(error -> error.contains(world.board.toString())),
                world.loader.last().errors()::toString);
        assertFalse(world.loader.last().actions().containsKey(world.board.toString()));
        assertTrue(speech.fired.isEmpty());
    }

    private void assertDelayAccepted(String iso, Duration expected) throws Exception {
        World world = world(delay(iso));
        StartupMappingsLoader.Loaded loaded = world.loader.load();
        assertTrue(loaded.errors().isEmpty(), loaded.errors()::toString);
        DelayAction delay = assertInstanceOf(DelayAction.class, loaded.actions().get(world.board.toString()).get(0));
        assertEquals(expected, delay.getDuration());
    }

    private void assertPermissionRefused(boolean file, PosixFilePermission bit, String node, String phrase) throws Exception {
        World world = world(gpio("fridge", true));
        Path target = file ? world.file : world.file.getParent();
        var permissions = EnumSet.copyOf(Files.getPosixFilePermissions(target));
        permissions.add(bit);
        Files.setPosixFilePermissions(target, permissions);
        world.run();
        assertTrue(world.harness.writes.isEmpty());
        String error = world.loader.last().errors().get(0);
        assertTrue(error.contains(node), error);
        assertTrue(error.contains(phrase), error);
    }

    private World world(String actions) throws Exception {
        return world(actions, new Gate(), "");
    }

    private World world(String actions, StartupActionRunner.DelayWait wait) throws Exception {
        return world(actions, wait, "");
    }

    private World world(String actions, StartupActionRunner.DelayWait wait, String controlMap) throws Exception {
        UUID board = UUID.randomUUID();
        Path file = writeMappings(document(board, APPLIANCES, controlMap, actions));
        return worldFrom(file, board, wait);
    }

    private World worldFrom(String json, UUID board, StartupActionRunner.DelayWait wait) throws Exception {
        return worldFrom(writeMappings(json), board, wait);
    }

    private World worldFrom(Path file, UUID board) throws Exception {
        return worldFrom(file, board, new Gate());
    }

    private World worldFrom(Path file, UUID board, StartupActionRunner.DelayWait wait) {
        Harness boardHarness = new Harness(board);
        harnesses.add(boardHarness);
        SimpleAliasService aliases = new SimpleAliasService();
        aliases.connectorService = boardHarness;
        aliases.mappingsFile = file.toString();
        aliases.digitalPinCache = new LinkedHashMap<>();
        StartupMappingsLoader loader = new StartupMappingsLoader(validator, file);
        StartupActionService service = new StartupActionService(boardHarness, aliases, speech, loader, wait);
        boardHarness.startupActions = service;
        boardHarness.publishConnectedBoard(boardHarness.connector, new ArrayList<>());
        boardHarness.published = true;
        return new World(board, file, boardHarness, aliases, loader, service);
    }

    private Path writeMappings(String json) throws IOException {
        Path dir = Files.createTempDirectory("startup-actions");
        ownerOnlyDirectory(dir);
        trash.add(dir);
        Path file = dir.resolve("mappings_v2.json");
        Files.writeString(file, json);
        Files.setPosixFilePermissions(file, EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
        return file;
    }

    private static void ownerOnlyDirectory(Path dir) throws IOException {
        Files.setPosixFilePermissions(dir, EnumSet.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE));
    }

    private static String document(UUID board, String appliances, String controlMap, String actions) {
        return """
                {
                  "eventTTL": 7,
                  "eventTTLUnit": "DAYS",
                  "mappings": [{
                    "connectorId": "%s",
                    "appliances": [%s],
                    "controlMap": {%s}
                  }],
                  "startupActions": {
                    "%s": [%s]
                  }
                }
                """.formatted(board, appliances, controlMap, board, actions);
    }

    private static String gpio(String name, boolean on) {
        return "{\"type\":\"gpio\",\"appliance\":\"" + name + "\",\"on\":" + on + "}";
    }

    private static String delay(String iso) {
        return "{\"type\":\"delay\",\"duration\":\"" + iso + "\"}";
    }

    private static void deleteTree(Path path) throws IOException {
        if (path == null || !Files.exists(path, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        if (Files.isSymbolicLink(path)) {
            Files.deleteIfExists(path);
            return;
        }
        if (Files.isDirectory(path)) {
            try (var children = Files.list(path)) {
                for (Path child : children.toList()) {
                    deleteTree(child);
                }
            }
        }
        Files.deleteIfExists(path);
    }

    private record World(
            UUID board,
            Path file,
            Harness harness,
            SimpleAliasService aliases,
            StartupMappingsLoader loader,
            StartupActionService service) {
        void run() {
            service.onBoardEnumerated(board);
        }
    }

    private static final class Gate implements StartupActionRunner.DelayWait {
        private final CountDownLatch entered = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);
        private int waits;
        private boolean lockHeld;
        private Harness harness;

        private void watch(Harness boardHarness) {
            harness = boardHarness;
        }

        @Override
        public boolean await(Duration duration, BooleanSupplier stillConnected) {
            waits++;
            lockHeld = harness != null && harness.boardLockHoldCount() > 0;
            if (waits > 1) {
                return stillConnected.getAsBoolean();
            }
            entered.countDown();
            try {
                if (!release.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("delay was not released");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
            return stillConnected.getAsBoolean();
        }
    }

    private final class Harness extends ConnectorService {
        private FakeConnector connector;
        private boolean handshakeInterrupted;
        private boolean published;
        private boolean reconnecting;
        private boolean failNext;
        private boolean timeoutAfterApply;
        private final Map<Identifier, Boolean> pins = new LinkedHashMap<>();
        private final List<Integer> writes = new ArrayList<>();
        private final List<Integer> holdCounts = new ArrayList<>();

        private Harness(UUID boardId) {
            connector = new FakeConnector(boardId);
        }

        @Override
        protected void discoverDevices(Collection<UUID> newlyConnected) {
            if (handshakeInterrupted) {
                return;
            }
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

        @Override
        public Response send(UUID uuid, Request request) {
            return withBoardLock(() -> {
                holdCounts.add(boardLockHoldCount());
                if (timeoutAfterApply) {
                    apply(request);
                    timeoutAfterApply = false;
                    throw new ConnectorTimeoutException("serial response", Duration.ofSeconds(2));
                }
                if (failNext) {
                    failNext = false;
                    throw new IllegalStateException("board rejected the request");
                }
                apply(request);
                return Response.builder().uuid(uuid).digital(new LinkedHashMap<>(pins)).build();
            });
        }

        private void apply(Request request) {
            if (request.write() == null) {
                return;
            }
            request.write().digital().forEach((pin, state) -> {
                pins.put(pin, state.getValue());
                writes.add(pin.pin());
            });
        }
    }

    private static final class FakeConnector implements Connector {
        private final UUID id;
        private volatile boolean closed;

        private FakeConnector(UUID id) {
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
        public Response getResponse(UUID requestId, Duration maxWait) {
            return Response.empty();
        }

        @Override
        public String device() {
            return "/dev/fake-" + id;
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

    /** Expected uid is not this process, so a file we own fails the owner check. */
    private static final class MismatchUidLoader extends StartupMappingsLoader {
        private MismatchUidLoader(Validator validator, Path file) {
            super(validator, file);
        }

        @Override
        protected long expectedUid() throws IOException {
            long actual = super.expectedUid();
            return actual == 0 ? 1 : 0;
        }
    }

    private static final class RecordingSpeech implements Event<SpeechEvent> {
        private final List<SpeechEvent> fired = new ArrayList<>();

        @Override
        public void fire(SpeechEvent event) {
            fired.add(event);
        }

        @Override
        public <U extends SpeechEvent> CompletionStage<U> fireAsync(U event) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <U extends SpeechEvent> CompletionStage<U> fireAsync(U event, jakarta.enterprise.event.NotificationOptions options) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Event<SpeechEvent> select(Annotation... qualifiers) {
            return this;
        }

        @Override
        public <U extends SpeechEvent> Event<U> select(Class<U> subtype, Annotation... qualifiers) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <U extends SpeechEvent> Event<U> select(TypeLiteral<U> subtype, Annotation... qualifiers) {
            throw new UnsupportedOperationException();
        }
    }
}
