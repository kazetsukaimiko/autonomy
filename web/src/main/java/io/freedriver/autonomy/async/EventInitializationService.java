package io.freedriver.autonomy.async;

import io.freedriver.autonomy.entity.event.input.joystick.JoystickEvent;
import io.freedriver.autonomy.entity.event.input.joystick.jstest.AllJoysticks;
import io.freedriver.autonomy.entity.event.input.joystick.jstest.JSTestEvent;
import io.freedriver.autonomy.service.ConnectorService;
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
public class EventInitializationService extends BaseService {

    private static final Logger LOGGER = Logger.getLogger(EventInitializationService.class.getName());

    private static final Map<VEDirectDevice, Future<Boolean>> devicesInOperation = new ConcurrentHashMap<>();

    @Inject
    private ConnectorService connectorService;

    @Inject
    private VEDirectDeviceService deviceService;

    @Inject
    private Event<JoystickEvent> joystickEvents;

    @Inject
    private Event<VEDirectMessage> veDirectEvents;

    @Resource
    private ManagedExecutorService pool;

    private AllJoysticks allJoysticks;

    public void init(@Observes @Initialized(ApplicationScoped.class) Object init) {
        try {
            connectorService.setWorkspace(connectorService.generateFromMappings());
        } catch (IOException e) {
            LOGGER.log(Level.INFO, "Trouble generating configuration", e);
        }
        initJoystickMonitor();
        initVEDirectMonitor();
    }

    private boolean initVEDirectMonitor() {
        LOGGER.info("Initializing VEDirectMonitor.");
        while (true) {
            try {
                deviceService.allDevices()
                        .filter(this::veDeviceInactive)
                        .forEach(this::initVEDirectDevice);
                break;
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Couldn't iterate over VEDirectDevices:", e);
                wait(Duration.of(5, ChronoUnit.SECONDS));
            }

        }
        LOGGER.info("VEDirectMonitor initialized.");
        return true;
    }

    private boolean veDeviceInactive(VEDirectDevice veDirectDevice) {
        return Optional.of(veDirectDevice)
                .filter(devicesInOperation::containsKey)
                .map(devicesInOperation::get)
                .map(Future::isDone)
                .orElse(true);
    }

    private synchronized void initVEDirectDevice(final VEDirectDevice veDirectDevice) {
        LOGGER.info("Initializing VEDirectDevice: " + veDirectDevice.toString());
        devicesInOperation.put(veDirectDevice, pool.submit(() -> {
            veDirectDevice.readAsMessages()
                    .forEach(this::fireVEDirectMessage);
            return initVEDirectMonitor();
        }));
    }

    public void initJoystickMonitor() {
        LOGGER.info("Initializing JoystickMonitor.");
        allJoysticks = new AllJoysticks(pool, this::fireJSTestEvent);
        pool.submit(() -> allJoysticks.poll());
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

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
