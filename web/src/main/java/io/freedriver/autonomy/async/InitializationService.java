package io.freedriver.autonomy.async;

import io.freedriver.autonomy.config.Configuration;
import io.freedriver.autonomy.entity.JoystickEvent;
import io.freedriver.autonomy.jstest.AllJoysticks;
import io.freedriver.autonomy.jstest.JSTestEvent;
import io.freedriver.jsonlink.Connector;

import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class InitializationService {

    private static final Logger LOGGER = Logger.getLogger(InitializationService.class.getName());

    //@Inject
    //private Configuration configuration;

    @Inject
    private Connector connector;

    @Inject
    private Event<JoystickEvent> joystickEvents;

    @Resource
    private ManagedExecutorService pool;

    private AllJoysticks allJoysticks;

    public void init(@Observes @Initialized(ApplicationScoped.class) Object init) {
        LOGGER.info("Initializing JoystickMonitor.");
        allJoysticks = new AllJoysticks(pool, this::convertAndFire);
        pool.submit(() -> allJoysticks.populate());
    }

    public void convertAndFire(JSTestEvent jsTestEvent) {
        try {
            joystickEvents.fire(new JoystickEvent(jsTestEvent));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to fire JoystickEvent: ", e);
        }
    }
}
