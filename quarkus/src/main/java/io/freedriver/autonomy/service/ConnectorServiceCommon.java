package io.freedriver.autonomy.service;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.freedriver.jsonlink.Connector;
import io.freedriver.jsonlink.Connectors;
import io.freedriver.jsonlink.jackson.JsonLinkModule;
import io.freedriver.jsonlink.jackson.schema.v1.Request;
import io.freedriver.jsonlink.jackson.schema.v1.Response;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * The service by which we interact with connectors.
 *
 * <p>One {@link ReentrantLock} guards the open-connector list and the board
 * conversation. {@link #send}, discovery inside {@link #getAllConnectors}, and
 * the confirmed-output save all enter it through {@link #withBoardLock} and
 * through nothing else. The reset wait and UUID handshake run under that lock.
 * Appliance restore runs only after the lock is released, then {@code setState}
 * takes the same lock again to write the saved outputs. Callers must not
 * synchronize on this bean or on its client proxy.
 */
@ApplicationScoped
@Slf4j
public class ConnectorServiceCommon {
    private static final List<Connector> ACTIVE_CONNECTORS = new CopyOnWriteArrayList<>();
    private final ReentrantLock boardLock = new ReentrantLock();
    private final ThreadLocal<ConcurrentLinkedQueue<UUID>> restoreAfterUnlock =
            ThreadLocal.withInitial(ConcurrentLinkedQueue::new);
    private static final Path CONFIG_PATH = Paths.get(System.getProperty("user.home"), ".config/autonomy");
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JsonLinkModule())
            .enable(SerializationFeature.INDENT_OUTPUT);


    protected ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    @Inject
    ApplianceRestoreService applianceRestoreService;

    /**
     * Opens boards that are not already connected. Each open waits out the board
     * reset and completes the UUID handshake before the connector is published,
     * and saved appliance state is restored after that.
     */
    public void refreshConnectedBoards() {
        getAllConnectors();
    }

    public List<UUID> getConnectedBoards() {
        return getAllConnectors().stream()
                .map(this::uuidOrNull)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Guards board discovery, sends, and the save that follows. Must not wrap slow work.
     * The outermost caller restores boards discovered while the lock was held, after releasing it.
     */
    <T> T withBoardLock(Supplier<T> action) {
        boolean outermost = !boardLock.isHeldByCurrentThread();
        boardLock.lock();
        try {
            return action.get();
        } finally {
            boardLock.unlock();
            if (outermost) {
                restoreNewlyConnected(drainBoards(restoreAfterUnlock.get()));
                restoreAfterUnlock.remove();
            }
        }
    }

    void withBoardLock(Runnable action) {
        withBoardLock(() -> {
            action.run();
            return null;
        });
    }

    private static List<UUID> drainBoards(Queue<UUID> pending) {
        List<UUID> boards = new ArrayList<>();
        UUID boardId;
        while ((boardId = pending.poll()) != null) {
            boards.add(boardId);
        }
        return boards;
    }

    protected boolean holdingBoardLock() {
        return boardLock.isHeldByCurrentThread();
    }

    /*
     * INTERNALS / HELPERS
     */
    protected List<Connector> getAllConnectors() {
        withBoardLock(this::openNewBoards);
        return ACTIVE_CONNECTORS;
    }

    /**
     * Drops closed connectors and opens new ones. Called with the board lock held.
     * The reset wait and UUID handshake happen here. Restore does not.
     */
    protected void openNewBoards() {
        List<Connector> closed = ACTIVE_CONNECTORS.stream()
                .filter(this::connectorIsClosed)
                .collect(Collectors.toList());
        ACTIVE_CONNECTORS.removeAll(closed);
        discoverDevices(restoreAfterUnlock.get());
    }

    /**
     * Opens each new serial device. {@code findOrOpenAndConsume} returns only after
     * the reset wait and UUID handshake. Called with the board lock held.
     */
    protected void discoverDevices(Collection<UUID> newlyConnected) {
        // Match by canonical path so /dev/serial/by-id/... and /dev/ttyACM0
        // are not opened twice against the same Arduino.
        List<CompletableFuture<Void>> threads = Connectors.allDevices().stream()
                .filter(device -> ACTIVE_CONNECTORS.stream()
                        .noneMatch(existing -> sameDevice(existing, device)))
                .map(device -> Connectors.findOrOpenAndConsume(
                        device, executorService, connector -> publishConnectedBoard(connector, newlyConnected)))
                .collect(Collectors.toList());
        threads.forEach(this::waitForCompletion);
    }

    /**
     * Marks the board awaiting restore before other callers can use it, so the
     * all-off snapshot after reset is not saved as a user change.
     * Discovery pool threads call this together, so {@code newlyConnected} is concurrent.
     */
    void publishConnectedBoard(Connector connector, Collection<UUID> newlyConnected) {
        UUID boardId = uuidOrNull(connector);
        if (boardId != null) {
            applianceRestoreService.beginAwaitingRestore(boardId);
            applianceRestoreService.dropStalePinCache(boardId);
            newlyConnected.add(boardId);
        }
        ACTIVE_CONNECTORS.add(connector);
    }

    void restoreNewlyConnected(List<UUID> newlyConnected) {
        if (newlyConnected.isEmpty()) {
            return;
        }
        try {
            applianceRestoreService.onBoardsReady(List.copyOf(newlyConnected));
        } catch (RuntimeException e) {
            log.warn("Couldn't restore appliances after board handshake", e);
        }
    }

    void forgetTrackedConnector(Connector connector) {
        ACTIVE_CONNECTORS.remove(connector);
    }

    private UUID uuidOrNull(Connector connector) {
        try {
            return connector.getUUID();
        } catch (Exception e) {
            log.warn("Couldn't read board UUID from {}", connector.device(), e);
            return null;
        }
    }

    private static boolean sameDevice(Connector existing, Path device) {
        return Objects.equals(canonical(existing.devicePath()), canonical(device));
    }

    private static Path canonical(Path path) {
        try {
            return path.toRealPath();
        } catch (IOException e) {
            return path.toAbsolutePath().normalize();
        }
    }

    protected boolean connectorIsClosed(Connector connector) {
        return connector.
                isClosed();
    }

    protected void waitForCompletion(CompletableFuture<Void> voidCompletableFuture) {
        try {
            voidCompletableFuture.get();
        } catch (Exception e) {
            log.warn("Failed to wait for completion of connector", e);
        }
    }

    protected Optional<Connector> getConnectorByBoardId(UUID boardId) {
        return ACTIVE_CONNECTORS.stream()
                .filter(connector -> Objects.equals(boardId, uuidOrNull(connector)))
                .findFirst();
    }

/*
    public String describeBoards() {
        return getAllConnectors().stream()
                .map(Connector::getUUID)
                .sorted(Comparator.comparing(UUID::toString))
                .map(UUID::toString)
                .collect(Collectors.joining(","));
    }*/

    public Response send(UUID uuid, Request request) {
        return withBoardLock(() -> {
            openNewBoards();
            return getConnectorByBoardId(uuid)
                    .map(connector -> connector.send(request))
                    .orElseThrow(() -> new BoardNotFoundException(
                            "Board not found, present devices: " + ACTIVE_CONNECTORS.stream()
                                    .map(Connector::device)
                                    .collect(Collectors.joining(","))));
        });
    }

    /*
    @Deprecated
    public synchronized Map<Identifier, Boolean> readDigital(UUID boardId, Collection<Identifier> pins) {
        return send(boardId, pins.stream()
                .reduce(Request.empty(), Request::digitalRead, (a, b) -> a))
                .digital();
    }

    public synchronized Response readDigitalAndAnalog(UUID boardId, Collection<Identifier> pins, Stream<AnalogRead> analogReads) {
        return send(boardId, pins.stream()
                .reduce(Request.empty(), Request::digitalRead, (a, b) -> a)
                .analogRead(analogReads));
    }

    public synchronized Map<Identifier, Boolean> writeDigital(UUID boardId, Map<Identifier, Boolean> state) {
        Request request = state.entrySet().stream()
                .reduce(Request.empty(),
                        (req, e) -> req.digitalWrite(new DigitalWrite(e.getKey(), e.getValue())),
                        (a, b) -> a);
        return send(boardId, request)
                .digital();
    }

     */
}
