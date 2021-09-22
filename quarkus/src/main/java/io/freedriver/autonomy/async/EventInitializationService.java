package io.freedriver.autonomy.async;

import io.freedriver.autonomy.event.input.joystick.jstest.AllJoysticks;
import io.freedriver.autonomy.jpa.entity.event.input.joystick.JoystickEvent;
import io.freedriver.autonomy.jpa.entity.event.input.joystick.jstest.JSTestEvent;
import io.freedriver.autonomy.service.SimpleAliasService;
import io.freedriver.electrodacus.sbms.SBMS0Finder;
import io.freedriver.electrodacus.sbms.SBMSMessage;
import io.freedriver.victron.VEDirectMessage;
import io.freedriver.victron.VEDirectStreamer;
import io.quarkus.runtime.StartupEvent;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class EventInitializationService extends BaseService {

    private static final Logger LOGGER = Logger.getLogger(EventInitializationService.class.getName());

    private final Map<VEDirectStreamer, Future<Boolean>> devicesInOperation = new ConcurrentHashMap<>();
    private final Map<Path, Future<Boolean>> sbmsUnits =  new ConcurrentHashMap<>();
    private final Map<Path, Instant> sbmsUnitsDead = new ConcurrentHashMap<>();

    private final ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()*10);

    private AllJoysticks allJoysticks;

    @Inject
    VEDirectDeviceService deviceService;

    @Inject
    SimpleAliasService simpleAliasService;

    @Inject
    Event<JoystickEvent> joystickEvents;

    @Inject
    Event<VEDirectMessage> veDirectEvents;

    @Inject
    Event<SBMSMessage> sbmsEvents;

    public void init(@Observes StartupEvent ev) {
        initSBMSMonitor();
        initJoystickMonitor();
        initVEDirectMonitor();
        initSimpleAliasMonitor();
    }

    private void initSimpleAliasMonitor() {
        LOGGER.info("Initializing SimpleAliasMonitor.");
        pool.submit(() -> {
            simpleAliasService.refreshAnalogPins();
        });
    }

    private void initSBMSMonitor() {
        LOGGER.info("Initializing SBMSMonitor.");
        pool.submit(() -> {
            while (true) {
                try {
                    Thread.sleep(1000);
                    SBMS0Finder.findSBMS0Units()
                            .forEach(this::addSBMS);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Couldn't init SBMS0 units", e);
                }
            }
        });
    }

    private boolean inSBMSDeadPeriod(Path unit) {
        try {
            if (sbmsUnitsDead.containsKey(unit)) {
                if (!Instant.now().isAfter(sbmsUnitsDead.get(unit))) {
                    return true;
                } else {
                    sbmsUnitsDead.remove(unit);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private void addSBMS(Path unit) {
        if (!sbmsUnits.containsKey(unit) || sbmsUnits.get(unit).isDone()) {
            if (!inSBMSDeadPeriod(unit)) {
                sbmsUnits.put(unit, pool.submit(() -> {
                    try {
                        SBMS0Finder.open(unit)
                                .forEach(this::fireSBMS0Message);
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Failed to stream messages: ", e);
                        Duration waitingPeriod = Duration.ofMinutes(1);
                        LOGGER.info("Blacklisting SBMS " + unit + " for " + waitingPeriod.toMillis() + "ms");
                        sbmsUnitsDead.put(unit, Instant.now().plus(waitingPeriod));
                    }
                    sbmsUnits.remove(unit);
                    return true;
                }));
            }
        }
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


    private boolean veDeviceInactive(VEDirectStreamer veDirectDevice) {
        return Optional.of(veDirectDevice)
                .filter(devicesInOperation::containsKey)
                .map(devicesInOperation::get)
                .map(Future::isDone)
                .orElse(true);
    }

    private synchronized void initVEDirectDevice(final VEDirectStreamer veDirectDevice) {
        LOGGER.info("Initializing VEDirectDevice: " + veDirectDevice.toString());
        devicesInOperation.put(veDirectDevice, pool.submit(() -> {
            veDirectDevice.stream()
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
        if (jsTestEvent.getMetadata().getTitle() != null) {
            try {
                LOGGER.finest("Firing JSTestEvent " + jsTestEvent);
                joystickEvents.fire(new JoystickEvent(Instant.now().toEpochMilli(), jsTestEvent));
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to fire JoystickEvent: " + jsTestEvent, e);
            }
        } else {
            // TODO: This is a workaround for a bug. Fix the bug.
            LOGGER.warning("JSTestEvent ignored as it contains no subject: " + jsTestEvent);
        }
    }

    private synchronized void fireSBMS0Message(SBMSMessage sbmsMessage) {
        try {
            LOGGER.finest("Firing SBMSMessage " + sbmsMessage);
            sbmsEvents.fire(sbmsMessage);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to fire SBMSMessage: " + sbmsMessage, e);
        }
    }

    private synchronized void fireVEDirectMessage(VEDirectMessage veDirectMessage) {
        try {
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
