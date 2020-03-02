package io.freedriver.autonomy.entity.event.input.joystick.jstest;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AllJoysticks implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(AllJoysticks.class.getName());

    private final Map<Path, Future<?>> activeJoysticks = new ConcurrentHashMap<>();
    private final Map<Path, FailedJoystick> failedJoystickMap = new ConcurrentHashMap<>();
    private final ExecutorService executorService;
    private final Consumer<JSTestEvent> sink;
    private final Function<Stream<JSTestEvent>, Stream<JSTestEvent>> addOns;

    private boolean open = true;

    public AllJoysticks(ExecutorService executorService, Consumer<JSTestEvent> sink) {
        this(executorService, sink, Function.identity());
    }

    public AllJoysticks(ExecutorService executorService, Consumer<JSTestEvent> sink, Function<Stream<JSTestEvent>, Stream<JSTestEvent>> addOns) {
        this.executorService = executorService;
        this.sink = sink;
        this.addOns = addOns;
    }

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

    public synchronized Map<Path, FailedJoystick> getFailedJoystickMap() {
        Set<FailedJoystick> toRemove = new HashSet<>(failedJoystickMap.values());
        toRemove.stream()
                .filter(FailedJoystick::failureExpired)
                .map(FailedJoystick::getPath)
                .forEach(failedJoystickMap::remove);
        return failedJoystickMap;
    }

    public void poll() {
        LOGGER.info("Polling Joysticks.");
        while (open) {
            populate();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new JSTestException("Interrupted: ", e);
            }
        }
    }

    private void populate() {
        JSTestReader.getJoysticksPaths().stream()
                .filter(this::shouldCreateReader)
                .forEach(this::constructFromPool);
    }

    private boolean shouldCreateReader(Path path) {
        if (!getActiveJoysticks().containsKey(path)) {
            if (!getFailedJoystickMap().containsKey(path)) {
                LOGGER.info("New Joystick: " + path.toString());
                return true;
            }
        }
        return false;
    }

    private synchronized void constructFromPool(Path path) {
        LOGGER.info("Joystick " + path.toString() + " joining pool");
        try {
            activeJoysticks.put(
                    path,
                    executorService.submit(() -> addOns.apply(JSTestReader.ofJoystick(path)).forEach(sink)));
        } catch (Exception e) {
            FailedJoystick failedJoystick = new FailedJoystick(path);
            LOGGER.log(Level.SEVERE, e, () -> "Failed to assemble Joystick at "
                    + path.toString() + " into the pool. "
                    + "Will retry in " + failedJoystick.getDelay().toMillis() + "ms");
            getFailedJoystickMap()
                    .put(path, failedJoystick);
        }
    }

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
    }
}
