package io.freedriver.autonomy.event.input.joystick.jstest;

import java.nio.file.Path;
import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import io.freedriver.autonomy.util.Delayable;
import lombok.extern.slf4j.Slf4j;

/**
 * Single point to discover and monitor all joystick devices on a system.
 */
@Slf4j
public class AllJoysticks implements AutoCloseable {

    private final Map<Path, Future<?>> activeJoysticks = new ConcurrentHashMap<>();
    private final Map<Path, FailedJoystick> failedJoystickMap = new ConcurrentHashMap<>();
    private final ExecutorService executorService;
    private final Consumer<JSTestEvent> sink;
    private final Function<Stream<JSTestEvent>, Stream<JSTestEvent>> addOns;

    private boolean open = true;


    /**
     * Constructor.
     * @param executorService The Thread pool to spawn readers on.
     * @param sink Where the readers write events to.
     */
    public AllJoysticks(ExecutorService executorService, Consumer<JSTestEvent> sink) {
        this(executorService, sink, Function.identity());
    }

    /**
     * Constructor.
     * @param executorService The Thread pool to spawn readers on.
     * @param sink Where the readers write to.
     * @param addOns Stream chain modifiers to add
     */
    public AllJoysticks(ExecutorService executorService, Consumer<JSTestEvent> sink, Function<Stream<JSTestEvent>, Stream<JSTestEvent>> addOns) {
        this.executorService = executorService;
        this.sink = sink;
        this.addOns = addOns;
    }

    /**
     * Gets a Map of active Joystick readers keyed by their device path.
     * The value is the Future (thread) handling the reader.
     */
    public synchronized Map<Path, Future<?>> getActiveJoysticks() {
        Set<Path> toRemove = new HashSet<>();
        activeJoysticks.forEach((path, future) -> {
            if (future.isDone()) {
                toRemove.add(path);
            }
        });
        toRemove.forEach(activeJoysticks::remove);
        return activeJoysticks;
    }

    /**
     * Gets a map of Joystick devices that failed to spawn, keyed by their device path.
     * The value is an expiry for retrying the device.
     */
    public synchronized Map<Path, FailedJoystick> getFailedJoystickMap() {
        Set<FailedJoystick> toRemove = new HashSet<>(failedJoystickMap.values());
        toRemove.stream()
                .filter(FailedJoystick::failureExpired)
                .map(FailedJoystick::path)
                .forEach(failedJoystickMap::remove);
        return failedJoystickMap;
    }

    /**
     * Continuously poll for new joysticks, and add them to the pool.
     */
    public void poll() {
        log.info("Watching for Joysticks.");
        while (open) {
            populate();
            Delayable.wait(Duration.ofMillis(100));
        }
    }

    /**
     * Look for new joysticks to add to the pool.
     */
    private void populate() {
        JSTestReader.getJoysticksPaths().stream()
                .filter(this::shouldCreateReader)
                .forEach(this::constructFromPool);
    }

    /**
     * Decides if we should add the discovered joystick device file to the pool.
     */
    private boolean shouldCreateReader(Path path) {
        if (!getActiveJoysticks().containsKey(path)) {
            if (!getFailedJoystickMap().containsKey(path)) {
                log.info("New joystick: {}", path);
                return true;
            } else {
                log.info("Failed joystick: {}", path);
            }
        }
        return false;
    }

    /**
     * Creates a thread for submitting joystick events to the pool.
     */
    private synchronized void constructFromPool(Path path) {
        log.info("Joystick {} joining pool", path);
        try {
            activeJoysticks.put(
                    path,
                    executorService.submit(() -> {
                        addOns
                                .apply(JSTestReader
                                        .ofJoystick(path)
                                        .takeWhile(l -> open)).forEach(sink);
                        log.info("Closed Stream for Joystick at {}", path);
                    }));
        } catch (Exception e) {
            FailedJoystick failedJoystick = new FailedJoystick(path);
            log.error(
                    "Failed to assemble Joystick at {} into the pool. Will retry in {}ms",
                    path,
                    failedJoystick.getDelay().toMillis(),
                    e);
            getFailedJoystickMap()
                    .put(path, failedJoystick);
        }
    }

    /**
     * Waits for
     */
    public void waitForAllToClose() {
        while (true) {
            if (activeJoysticks.size() == 0 || activeJoysticks.entrySet().stream()
                    .allMatch(e -> e.getValue().isDone())) {
                break;
            }
        }
    }

    @Override
    public void close() throws Exception {
        open = false;
        waitForAllToClose();
    }
}