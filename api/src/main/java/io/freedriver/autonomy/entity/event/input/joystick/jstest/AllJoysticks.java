package io.freedriver.autonomy.entity.event.input.joystick.jstest;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class AllJoysticks implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(AllJoysticks.class.getName());

    private final Map<Path, Future<?>> activeJoysticks = new ConcurrentHashMap<>();
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
                .filter(path -> !activeJoysticks.containsKey(path) || !activeJoysticks.get(path).isDone())
                .forEach(this::constructFromPool);
    }

    private synchronized void constructFromPool(Path path) {
        LOGGER.info("Joystick " + path.toString() + " joining pool");
        try {
            executorService.submit(() -> addOns.apply(JSTestReader.ofJoystick(path)).forEach(sink));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e, e::getMessage);
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
