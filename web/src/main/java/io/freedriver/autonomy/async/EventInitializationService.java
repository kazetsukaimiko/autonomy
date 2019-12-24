package io.freedriver.autonomy.async;

import io.freedriver.autonomy.cdi.qualifier.ByUUID;
import io.freedriver.autonomy.entity.event.input.joystick.JoystickEvent;
import io.freedriver.autonomy.entity.event.input.joystick.jstest.AllJoysticks;
import io.freedriver.autonomy.entity.event.input.joystick.jstest.JSTestEvent;
import io.freedriver.jsonlink.Connector;
import kaze.victron.VEDirectDevice;
import kaze.victron.VEDirectMessage;

import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class EventInitializationService {

    private static final Logger LOGGER = Logger.getLogger(EventInitializationService.class.getName());

    private static final Map<VEDirectDevice, Future<Boolean>> devicesInOperation = new ConcurrentHashMap<>();

    // TODO: Use ByUUID.Literal. Static injection like this defeats the purpose.
    @Inject @ByUUID("af36c07b-20cf-4253-abb1-53280f13c211")
    private Connector connector;

    @Inject
    private Event<JoystickEvent> joystickEvents;

    @Inject
    private Event<VEDirectMessage> veDirectEvents;

    @Resource
    private ManagedExecutorService pool;

    private AllJoysticks allJoysticks;

    public void init(@Observes @Initialized(ApplicationScoped.class) Object init) {
        initJoystickMonitor();
        initVEDirectMonitor();
    }

    private boolean initVEDirectMonitor() {
        LOGGER.info("Initializing VEDirectMonitor.");
        while (true) {
            try {
                VEDirectDevice.allVEDirectDevices()
                        .filter(this::veDeviceInactive)
                        .forEach(this::initVEDirectDevice);
                break;
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Couldn't iterate over VEDirectDevices:", e);
                wait(Duration.of(5, ChronoUnit.SECONDS));
            }
        }
        return true;
    }

    private boolean veDeviceInactive(VEDirectDevice veDirectDevice) {
        return Optional.of(veDirectDevice)
                .filter(devicesInOperation::containsKey)
                .map(devicesInOperation::get)
                .map(Future::isDone)
                .orElse(false);
    }

    private synchronized void initVEDirectDevice(final VEDirectDevice veDirectDevice) {
        devicesInOperation.put(veDirectDevice, pool.submit(() -> {
            veDirectDevice.readAsMessages()
                    .forEach(this::fireVEDirectMessage);
            return initVEDirectMonitor();
        }));
    }

    public void initJoystickMonitor() {
        LOGGER.info("Initializing JoystickMonitor.");
        allJoysticks = new AllJoysticks(pool, this::fireJSTestEvent);
        pool.submit(() -> allJoysticks.populate());
    }

    public void fireJSTestEvent(JSTestEvent jsTestEvent) {
        try {
            joystickEvents.fire(new JoystickEvent(Instant.now(), jsTestEvent));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to fire JoystickEvent: ", e);
        }
    }

    private synchronized void fireVEDirectMessage(VEDirectMessage veDirectMessage) {
        try {
            veDirectEvents.fire(veDirectMessage);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to fire VEDirectMessage: ", e);
        }
    }

    private void wait(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Failed wait: ", e);
        }
    }
}
