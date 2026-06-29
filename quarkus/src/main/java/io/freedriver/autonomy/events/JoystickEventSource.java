package io.freedriver.autonomy.events;

import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import io.freedriver.autonomy.event.input.joystick.jstest.AllJoysticks;
import io.freedriver.autonomy.jpa.entity.event.input.joystick.JoystickEvent;
import io.freedriver.autonomy.jpa.entity.event.input.joystick.jstest.JSTestEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class JoystickEventSource implements EventSource {

    @Inject
    Event<JoystickEvent> joystickEvents;

    private ExecutorService executor;
    private AllJoysticks allJoysticks;
    private volatile boolean running;

    @Override
    public String name() {
        return "joystick";
    }

    @Override
    public void start() {
        running = true;
        executor = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r, "joystick-event-source");
            thread.setDaemon(true);
            return thread;
        });
        allJoysticks = new AllJoysticks(executor, this::handleJSTestEvent);
        executor.submit(() -> allJoysticks.poll());
    }

    @Override
    public void stop() {
        running = false;
        if (allJoysticks != null) {
            try {
                allJoysticks.close();
            } catch (Exception e) {
                log.debug("Error closing joystick source", e);
            }
        }
        if (executor != null) {
            executor.shutdownNow();
            try {
                executor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void handleJSTestEvent(JSTestEvent jsTestEvent) {
        if (jsTestEvent.getMetadata().getTitle() == null) {
            log.warn("JSTestEvent ignored as it contains no subject: {}", jsTestEvent);
            return;
        }
        try {
            log.trace("Firing JSTestEvent {}", jsTestEvent);
            joystickEvents.fire(new JoystickEvent(Instant.now().toEpochMilli(), jsTestEvent));
        } catch (Exception e) {
            log.warn("Failed to fire JoystickEvent: {}", jsTestEvent, e);
        }
    }
}