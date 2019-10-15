package io.freedriver.autonomy.jstest;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AllJoysticks {
    private static final Logger LOGGER = Logger.getLogger(AllJoysticks.class.getName());

    private final Map<Path, JSTestReader> activeJoysticks = new ConcurrentHashMap<>();
    private final Consumer<JSTestEvent> sink;

    public AllJoysticks(Consumer<JSTestEvent> sink) {
        this.sink = sink;
    }

    public void populate() {
        LOGGER.info("Populating Joysticks.");
        while (true) {
            JSTestReader.getJoysticksPaths().stream()
                    .filter(path -> !activeJoysticks.containsKey(path) || activeJoysticks.get(path).isDead())
                    .forEach(this::constructFromPool);
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw new JSTestException("Interrupted: ", e);
            }
        }

    }

    private synchronized void constructFromPool(Path path) {
        LOGGER.info("Joystick " + path.toString() + " joining pool");
        try {
            final JSTestReader joystickReader = new JSTestReader(path, sink);
            activeJoysticks.put(path, joystickReader);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e, e::getMessage);
        }
    }
}
