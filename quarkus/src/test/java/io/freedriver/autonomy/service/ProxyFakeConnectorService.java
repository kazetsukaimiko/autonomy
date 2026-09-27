package io.freedriver.autonomy.service;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import io.freedriver.jsonlink.Connector;
import io.freedriver.jsonlink.ConnectorException;
import io.freedriver.jsonlink.jackson.schema.v1.Identifier;
import io.freedriver.jsonlink.jackson.schema.v1.Request;
import io.freedriver.jsonlink.jackson.schema.v1.Response;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

/**
 * Test board used in place of a serial connector. Methods are the only way in,
 * so a Quarkus client proxy still reaches this instance.
 */
@Alternative
@ApplicationScoped
public class ProxyFakeConnectorService extends ConnectorService {

    static final UUID BOARD_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");

    private final Board board = new Board();
    private final ConcurrentLinkedQueue<Long> toggleHoldNanos = new ConcurrentLinkedQueue<>();
    private volatile boolean armed;
    private volatile boolean forceReconnect;
    private volatile boolean recordToggleHolds;
    private volatile Thread slowThread;
    private volatile CountDownLatch entered;
    private volatile CountDownLatch release;
    private boolean published;

    public void arm() {
        armed = true;
    }

    public void requestReconnect() {
        forceReconnect = true;
    }

    public void prepareSlowSendForCurrentThread(CountDownLatch entered, CountDownLatch release) {
        this.entered = entered;
        this.release = release;
        this.slowThread = Thread.currentThread();
    }

    public void startHoldSamples() {
        toggleHoldNanos.clear();
        recordToggleHolds = true;
    }

    public void stopHoldSamples() {
        recordToggleHolds = false;
    }

    public List<Long> toggleHoldNanos() {
        return List.copyOf(toggleHoldNanos);
    }

    public Boolean pin(Identifier identifier) {
        return withBoardLock(() -> board.pins.get(identifier));
    }

    public void disarm() {
        withBoardLock(() -> {
            armed = false;
            forceReconnect = false;
            slowThread = null;
            if (published) {
                forgetTrackedConnector(board);
                published = false;
            }
            board.pins.clear();
            return null;
        });
    }

    @Override
    public <T> T withBoardLock(Supplier<T> action) {
        boolean outermost = !holdingBoardLock();
        long started = System.nanoTime();
        try {
            return super.withBoardLock(action);
        } finally {
            if (outermost && recordToggleHolds && Thread.currentThread().getName().startsWith("toggle-")) {
                toggleHoldNanos.add(System.nanoTime() - started);
            }
        }
    }

    @Override
    protected void discoverDevices(Collection<UUID> newlyConnected) {
        if (!armed) {
            return;
        }
        if (forceReconnect) {
            forceReconnect = false;
            forgetTrackedConnector(board);
            published = false;
        }
        if (!published) {
            publishConnectedBoard(board, newlyConnected);
            published = true;
        }
    }

    private final class Board implements Connector {
        private final Map<Identifier, Boolean> pins = new LinkedHashMap<>();

        @Override
        public Response send(Request request) {
            Thread waiter = slowThread;
            if (waiter != null && waiter == Thread.currentThread()) {
                slowThread = null;
                CountDownLatch arrived = entered;
                CountDownLatch resume = release;
                if (arrived != null) {
                    arrived.countDown();
                }
                if (resume != null) {
                    try {
                        if (!resume.await(15, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("slow send was not released");
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(e);
                    }
                }
            }
            if (request.write() != null && request.write().digital() != null) {
                request.write().digital().forEach((pin, state) -> pins.put(pin, state.getValue()));
            }
            return Response.builder().uuid(BOARD_ID).digital(Map.copyOf(pins)).build();
        }

        @Override
        public UUID getUUID() {
            return BOARD_ID;
        }

        @Override
        public UUID makeRequest(Request request, Duration maxWait) {
            return UUID.randomUUID();
        }

        @Override
        public Response getResponse(UUID requestId, Duration maxWait) throws ConnectorException {
            return Response.builder().uuid(BOARD_ID).build();
        }

        @Override
        public String device() {
            return "/dev/fake-proxy-board";
        }

        @Override
        public Path devicePath() {
            return Path.of(device());
        }

        @Override
        public boolean isClosed() {
            return false;
        }

        @Override
        public void close() {
        }
    }
}
