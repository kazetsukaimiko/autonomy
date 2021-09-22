package io.freedriver.autonomy.microservices.vedirect.event;

import io.freedriver.autonomy.microservices.base.BaseService;
import io.freedriver.electrodacus.sbms.SBMS0Finder;
import io.freedriver.victron.VEDirectMessage;
import io.freedriver.victron.VEDirectStreamer;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

@ApplicationScoped
public class VEDirectDeviceService extends BaseService {
    private static final Logger LOGGER = Logger.getLogger(VEDirectDeviceService.class.getName());
    private final Map<VEDirectStreamer, Future<?>> devicesInOperation = new ConcurrentHashMap<>();

    private boolean running = true;

    @Inject
    Event<VEDirectMessage> veDirectEvents;

    /**
     * Quarkus Initializer
     * @param ev
     */
    public void init(@Observes StartupEvent ev) {
        pool.submit(() -> {
            LOGGER.info("Initializing VEDirectDeviceService.");
            while (running) {
                try {
                    allDevices()
                            .filter(this::veDeviceInactive)
                            .forEach(this::initVEDirectDevice);
                    wait(Duration.ofSeconds(1));
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Couldn't iterate over VEDirectDevices:", e);
                    wait(Duration.of(5, ChronoUnit.SECONDS));
                }

            }
            LOGGER.info("VEDirectDeviceService shutdown.");
        });
    }

    public void destroy(@Observes ShutdownEvent ev) {
        running = false;
    }

    public synchronized Stream<VEDirectStreamer> allDevices() {
        try {
            VEDirectStreamer.allMessageStreamers(SBMS0Finder::noMatch)
                    .filter(this::veDeviceInactive)
                    .forEach(this::initVEDirectDevice);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Couldn't iterate over VEDirectDevices:", e);
        }
        return devicesInOperation.keySet().stream();
    }

    /**
     * Predicate function to determine if the VEDirectDevice is already active or not
     * @param veDirectDevice
     * @return
     */
    private boolean veDeviceInactive(VEDirectStreamer veDirectDevice) {
        return deviceThread(veDirectDevice)
                .isEmpty();
    }

    /**
     *
     * @param veDirectReader
     * @return
     */
    private Optional<Future<?>> deviceThread(VEDirectStreamer veDirectReader) {
        Optional<Future<?>> thread = Optional.ofNullable(devicesInOperation.get(veDirectReader));
        if (thread.isPresent()) {
            if (!thread.get().isDone() && !thread.get().isCancelled()) {
                return thread;
            }
        }
        return Optional.empty();
    }

    private synchronized void initVEDirectDevice(final VEDirectStreamer veDirectDevice) {
        LOGGER.info("Initializing VEDirectDevice: " + veDirectDevice.toString());
        devicesInOperation.put(veDirectDevice, pool.submit(() -> {
            veDirectDevice.stream()
                    .takeWhile(this::continueRunning)
                    .forEach(this::fireVEDirectMessage);
            LOGGER.info("Stream done for " + veDirectDevice.getFirstMessage().getProductType());
        }));
    }

    private boolean continueRunning(VEDirectMessage veDirectMessage) {
        if (!running) {
            LOGGER.info("Shutting down stream for VEDirect Device: " + veDirectMessage.getProductType() + " " + veDirectMessage.getSerialNumber());
        }
        return running;
    }

    private synchronized void fireVEDirectMessage(VEDirectMessage veDirectMessage) {
        try {
            // TODO Remove
            LOGGER.info("Firing message for VEDirect Device: " + veDirectMessage.getProductType() + " " + veDirectMessage.getSerialNumber());
            veDirectEvents.fire(veDirectMessage);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to fire VEDirectMessage: " + veDirectMessage, e);
        }
    }


    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
