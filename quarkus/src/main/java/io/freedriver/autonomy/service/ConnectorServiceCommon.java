package io.freedriver.autonomy.service;

import java.io.IOException;
import java.nio.file.Path;
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
import io.freedriver.autonomy.service.startup.StartupActionService;
import io.freedriver.jsonlink.Connector;
import io.freedriver.jsonlink.Connectors;
import io.freedriver.jsonlink.jackson.JsonLinkModule;
import io.freedriver.jsonlink.jackson.schema.v1.Request;
import io.freedriver.jsonlink.jackson.schema.v1.Response;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import lombok.extern.slf4j.Slf4j;

/**
 * The service by which we interact with connectors.
 *
 * <p>One lock covers discovery and sends. Startup actions hold it and release it
 * for a delay, so REST and the joystick can send while that delay runs.
 * Callers must not synchronize on this bean.
 */
@ApplicationScoped
@Slf4j
public class ConnectorServiceCommon {
    private static final List<Connector> ACTIVE_CONNECTORS = new CopyOnWriteArrayList<>();
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JsonLinkModule())
            .enable(SerializationFeature.INDENT_OUTPUT);

    private final ReentrantLock boardLock = new ReentrantLock();
    private final ThreadLocal<Queue<UUID>> enumeratedAfterUnlock =
            ThreadLocal.withInitial(ConcurrentLinkedQueue::new);
    private final ThreadLocal<Boolean> runningStartup = ThreadLocal.withInitial(() -> Boolean.FALSE);

    protected ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    @Inject
    StartupActionService startupActions;

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

    public boolean isBoardConnected(UUID boardId) {
        return ACTIVE_CONNECTORS.stream()
                .filter(connector -> !connectorIsClosed(connector))
                .anyMatch(connector -> Objects.equals(boardId, uuidOrNull(connector)));
    }

    public int boardLockHoldCount() {
        return boardLock.getHoldCount();
    }

    /**
     * Outermost caller runs startup actions for boards whose handshake just finished,
     * after releasing the lock.
     */
    public <T> T withBoardLock(Supplier<T> action) {
        boolean outermost = !boardLock.isHeldByCurrentThread();
        boardLock.lock();
        try {
            return action.get();
        } finally {
            boardLock.unlock();
            if (outermost) {
                List<UUID> boards = drain(enumeratedAfterUnlock.get());
                enumeratedAfterUnlock.remove();
                runStartupActions(boards);
            }
        }
    }

    public void withBoardLock(Runnable action) {
        withBoardLock(() -> {
            action.run();
            return null;
        });
    }

    /** Drops every hold this thread has, runs {@code action}, then takes those holds back. */
    public <T> T callWithoutBoardLock(Interruptible<T> action) throws InterruptedException {
        int holds = boardLock.getHoldCount();
        for (int i = 0; i < holds; i++) {
            boardLock.unlock();
        }
        try {
            return action.call();
        } finally {
            for (int i = 0; i < holds; i++) {
                boardLock.lock();
            }
        }
    }

    protected List<Connector> getAllConnectors() {
        withBoardLock(this::openNewBoards);
        return ACTIVE_CONNECTORS;
    }

    /** Drops closed connectors and opens new ones. Called with the board lock held. */
    protected void openNewBoards() {
        List<Connector> closed = ACTIVE_CONNECTORS.stream()
                .filter(this::connectorIsClosed)
                .collect(Collectors.toList());
        ACTIVE_CONNECTORS.removeAll(closed);
        discoverDevices(enumeratedAfterUnlock.get());
    }

    /**
     * Opens each new serial device. The callback runs only after the UUID handshake.
     * An interrupted handshake does not publish the board.
     */
    protected void discoverDevices(Collection<UUID> newlyConnected) {
        List<CompletableFuture<Void>> threads = Connectors.allDevices().stream()
                .filter(device -> ACTIVE_CONNECTORS.stream()
                        .noneMatch(existing -> sameDevice(existing, device)))
                .map(device -> Connectors.findOrOpenAndConsume(
                        device, executorService, connector -> publishConnectedBoard(connector, newlyConnected)))
                .collect(Collectors.toList());
        threads.forEach(this::waitForCompletion);
    }

    protected void publishConnectedBoard(Connector connector, Collection<UUID> newlyConnected) {
        UUID boardId = uuidOrNull(connector);
        if (boardId != null) {
            newlyConnected.add(boardId);
        }
        ACTIVE_CONNECTORS.add(connector);
    }

    protected void forgetTrackedConnector(Connector connector) {
        ACTIVE_CONNECTORS.remove(connector);
    }

    private void runStartupActions(List<UUID> boards) {
        if (startupActions == null || boards.isEmpty() || runningStartup.get()) {
            return;
        }
        runningStartup.set(Boolean.TRUE);
        try {
            for (UUID boardId : boards) {
                try {
                    startupActions.onBoardEnumerated(boardId);
                } catch (RuntimeException e) {
                    log.error("startupActions failed for board {}", boardId, e);
                }
            }
        } finally {
            runningStartup.remove();
        }
    }

    private static List<UUID> drain(Queue<UUID> pending) {
        List<UUID> boards = new ArrayList<>();
        UUID boardId;
        while (pending != null && (boardId = pending.poll()) != null) {
            if (!boards.contains(boardId)) {
                boards.add(boardId);
            }
        }
        return boards;
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
        return connector.isClosed();
    }

    protected void waitForCompletion(CompletableFuture<Void> voidCompletableFuture) {
        try {
            voidCompletableFuture.get();
        } catch (Exception e) {
            log.warn("Failed to wait for completion of connector", e);
        }
    }

    protected Optional<Connector> findOpen(UUID boardId) {
        return ACTIVE_CONNECTORS.stream()
                .filter(connector -> !connectorIsClosed(connector))
                .filter(connector -> Objects.equals(boardId, uuidOrNull(connector)))
                .findFirst();
    }

    public Response send(UUID uuid, Request request) {
        return withBoardLock(() -> {
            Optional<Connector> connector = findOpen(uuid);
            if (connector.isEmpty()) {
                openNewBoards();
                connector = findOpen(uuid);
            }
            return connector
                    .map(found -> found.send(request))
                    .orElseThrow(() -> new WebApplicationException(
                            "Board not found, present devices: " + ACTIVE_CONNECTORS.stream()
                                    .map(Connector::device)
                                    .collect(Collectors.joining(",")),
                            404));
        });
    }

    @FunctionalInterface
    public interface Interruptible<T> {
        T call() throws InterruptedException;
    }
}
