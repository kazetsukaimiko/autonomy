package io.freedriver.autonomy.deployments.joystick;

import io.freedriver.autonomy.event.input.joystick.jstest.AllJoysticks;
import io.freedriver.autonomy.jpa.entity.event.input.joystick.JoystickEvent;
import io.freedriver.autonomy.jpa.entity.event.input.joystick.jstest.JSTestEvent;
import io.quarkus.runtime.StartupEvent;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@ApplicationScoped
public class JoystickEventService {

    private static final Logger LOGGER = Logger.getLogger(JoystickEventService.class);

    private final ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()*10);

    private AllJoysticks allJoysticks;

    @Inject
    Event<JoystickEvent> joystickEvents;

    public void init(@Observes StartupEvent ev) {
        LOGGER.info("Initializing JoystickMonitor.");

        allJoysticks = new AllJoysticks(pool, this::fireJSTestEvent);

        pool.submit(() -> allJoysticks.poll());
    }

    public void fireJSTestEvent(JSTestEvent jsTestEvent) {
        if (jsTestEvent.getMetadata().getTitle() != null) {
            try {
                LOGGER.debugf("Firing JSTestEvent %s", jsTestEvent);
                joystickEvents.fire(new JoystickEvent(Instant.now().toEpochMilli(), jsTestEvent));
            } catch (Exception e) {
                LOGGER.warnf(e, "Failed to fire JoystickEvent: %s", jsTestEvent);
            }
        } else {
            // TODO: This is a workaround for a bug. Fix the bug.
            LOGGER.warnf("JSTestEvent ignored as it contains no subject: %s", jsTestEvent);
        }
    }
}
