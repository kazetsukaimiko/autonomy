package io.freedriver.autonomy.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.freedriver.autonomy.ProxyBoardProfile;
import io.freedriver.autonomy.ProxyBoardTestResource;
import io.freedriver.jsonlink.jackson.schema.v1.Identifier;
import io.freedriver.jsonlink.jackson.schema.v1.Request;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Restore and the board lock, exercised through Quarkus client proxies.
 */
@QuarkusTest
@TestProfile(ProxyBoardProfile.class)
@QuarkusTestResource(ProxyBoardTestResource.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApplianceRestoreProxyTest {

    private static final Identifier FRIDGE = Identifier.of(40);
    private static final Identifier HALLWAY = Identifier.of(41);
    private static final Identifier OVEN = Identifier.of(42);

    @Inject
    ProxyFakeConnectorService boards;

    @Inject
    ConnectorService connectorService;

    @Inject
    SimpleAliasService aliases;

    @Inject
    ApplianceStateStore store;

    @Inject
    ApplianceRestoreService restore;

    @AfterEach
    void disarm() {
        boards.disarm();
    }

    @Test
    @Order(1)
    void handshakeRestoresSavedStatesThroughTheClientProxy() throws IOException {
        store.write(Map.of("fridge", true, "hallway", false, "oven", false));
        boards.arm();
        connectorService.refreshConnectedBoards();

        assertEquals(Boolean.TRUE, boards.pin(FRIDGE));
        assertEquals(Boolean.FALSE, boards.pin(HALLWAY));
        assertEquals(Boolean.FALSE, boards.pin(OVEN));
        assertEquals(Boolean.TRUE, store.load().states().get("fridge"));
        assertFalse(restore.isAwaitingRestore(ProxyFakeConnectorService.BOARD_ID));
    }

    @Test
    @Order(2)
    void eightThreadsLeaveTheFileMatchingTheInjectedBoard() throws Exception {
        boards.arm();
        connectorService.refreshConnectedBoards();
        boards.startHoldSamples();
        String[] names = {"fridge", "hallway", "oven"};
        int threads = 8;
        int toggles = 40;
        AtomicInteger threadNames = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(threads, runnable -> {
            Thread thread = new Thread(runnable, "toggle-" + threadNames.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        });
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int thread = 0; thread < threads; thread++) {
                int offset = thread;
                futures.add(pool.submit(() -> {
                    try {
                        for (int step = 0; step < toggles; step++) {
                            aliases.setState(
                                    ProxyFakeConnectorService.BOARD_ID,
                                    Map.of(names[(offset + step) % names.length], step % 2 == 0));
                        }
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                }));
            }
            for (Future<?> future : futures) {
                future.get();
            }
        } finally {
            pool.shutdownNow();
            boards.stopHoldSamples();
        }

        boards.withBoardLock(() -> {
            assertEquals(boards.pin(FRIDGE), store.load().states().get("fridge"));
            assertEquals(boards.pin(HALLWAY), store.load().states().get("hallway"));
            assertEquals(boards.pin(OVEN), store.load().states().get("oven"));
            return null;
        });
        List<Long> holds = new ArrayList<>(boards.toggleHoldNanos());
        assertFalse(holds.isEmpty());
        holds.sort(Long::compareTo);
        long medianMicros = holds.get(holds.size() / 2) / 1_000L;
        long maxMicros = holds.get(holds.size() - 1) / 1_000L;
        System.out.println("BOARD_LOCK_HOLD_MICROS median=" + medianMicros + " max=" + maxMicros + " n=" + holds.size());
    }

    @Test
    @Order(3)
    void hotplugDuringSendFinishes() throws Exception {
        boards.arm();
        connectorService.refreshConnectedBoards();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2, runnable -> {
            Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            return thread;
        });
        try {
            Future<?> slow = pool.submit(() -> {
                boards.prepareSlowSendForCurrentThread(entered, release);
                connectorService.send(ProxyFakeConnectorService.BOARD_ID, Request.empty());
                return null;
            });
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            boards.requestReconnect();
            Future<?> hotplug = pool.submit(() -> {
                connectorService.refreshConnectedBoards();
                return null;
            });
            Thread.sleep(300);
            assertFalse(hotplug.isDone(), "refresh finished while a send still held the board lock");
            release.countDown();
            slow.get(8, TimeUnit.SECONDS);
            hotplug.get(8, TimeUnit.SECONDS);
        } finally {
            release.countDown();
            pool.shutdownNow();
        }
    }
}
