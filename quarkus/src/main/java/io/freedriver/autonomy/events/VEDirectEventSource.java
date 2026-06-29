package io.freedriver.autonomy.events;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import io.freedriver.serial.api.connection.SerialConnectionHandle;
import io.freedriver.serial.api.connection.SerialConnectionManager;
import io.freedriver.serial.api.connection.SerialDeviceIdentity;
import io.freedriver.serial.api.params.BaudRates;
import io.freedriver.serial.api.params.SerialParams;
import io.freedriver.victron.VEDirectMessage;
import io.freedriver.victron.VEDirectMessageStream;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class VEDirectEventSource implements EventSource {
    private static final String VICTRON_BY_ID_PREFIX = "usb-VictronEnergy_BV_VE_Direct_cable";
    private static final SerialParams VEDIRECT_PARAMS =
            new SerialParams().setBaudRate(BaudRates.BAUDRATE_19200);

    private final Map<SerialDeviceIdentity, Future<?>> readers = new ConcurrentHashMap<>();

    @Inject
    SerialConnectionManager connectionManager;

    @Inject
    Event<VEDirectMessage> veDirectEvents;

    private ExecutorService executor;
    private volatile boolean running;

    @Override
    public String name() {
        return "vedirect";
    }

    @Override
    public void start() {
        running = true;
        executor = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r, "vedirect-event-source");
            thread.setDaemon(true);
            return thread;
        });
        connectionManager.start();
        executor.submit(this::discoveryLoop);
    }

    @Override
    public void stop() {
        running = false;
        if (executor != null) {
            executor.shutdownNow();
            try {
                executor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        readers.clear();
    }

    private void discoverNow() {
        connectionManager.discover(this::isVictronCable).forEach(this::ensureReader);
    }

    private void discoveryLoop() {
        while (running) {
            try {
                discoverNow();
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.warn("VEDirect discovery failed", e);
            }
        }
    }

    private boolean isVictronCable(Path byIdPath) {
        return byIdPath.getFileName().toString().startsWith(VICTRON_BY_ID_PREFIX);
    }

    private void ensureReader(SerialDeviceIdentity identity) {
        Future<?> existing = readers.get(identity);
        if (existing != null && !existing.isDone()) {
            return;
        }
        readers.put(identity, executor.submit(() -> readForever(identity)));
    }

    private void readForever(SerialDeviceIdentity identity) {
        log.info("Starting VEDirect reader for {}", identity);
        SerialConnectionHandle handle = connectionManager.connect(identity, VEDIRECT_PARAMS);
        try {
            new VEDirectMessageStream(handle.resource())
                    .stream()
                    .forEach(this::publish);
        } catch (Exception e) {
            if (running) {
                log.warn("VEDirect reader ended for {}", identity, e);
            }
        } finally {
            readers.remove(identity);
        }
    }

    private void publish(VEDirectMessage message) {
        try {
            veDirectEvents.fire(message);
        } catch (Exception e) {
            log.warn("Failed to fire VEDirectMessage: {}", message, e);
        }
    }
}