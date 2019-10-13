package io.freedriver.controller;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AllJoysticks {
    private static final Logger LOGGER = Logger.getLogger(AllJoysticks.class.getName());

    private final Map<Path, JoystickReader> activeJoysticks = new ConcurrentHashMap<>();
    private final Consumer<JoystickEvent> sink;

    public AllJoysticks(Consumer<JoystickEvent> sink) {
        this.sink = sink;
    }

    public void populate() {
        System.out.println("populate");
        while (true) {
            JoystickReader.getJoysticksPaths().stream()
                    .filter(path -> !activeJoysticks.containsKey(path) || activeJoysticks.get(path).isDead())
                    .forEach(this::constructFromPool);
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw new JoystickReaderException("Interrupted: ", e);
            }
        }

    }

    private synchronized void constructFromPool(Path path) {
        try {
            final JoystickReader joystickReader = new JoystickReader(path, sink);
            activeJoysticks.put(path, joystickReader);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e, e::getMessage);
        }
    }
}
