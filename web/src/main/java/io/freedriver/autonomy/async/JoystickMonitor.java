package io.freedriver.autonomy.async;

import io.freedriver.controller.AllJoysticks;
import io.freedriver.controller.JoystickEvent;

import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.logging.Logger;

@ApplicationScoped
public class JoystickMonitor {

    private static final Logger LOGGER = Logger.getLogger(JoystickMonitor.class.getName());

    @Inject
    Event<JoystickEvent> joystickEvents;

    @Resource
    private ManagedExecutorService pool;

    private AllJoysticks allJoysticks;

    public void init(@Observes @Initialized(ApplicationScoped.class) Object init) {
        System.out.println("Heeeeeelllllllllllooooooooooo");
        allJoysticks = new AllJoysticks(this::sense);
        pool.submit(() -> allJoysticks.populate());
    }

    public void sense(JoystickEvent joystickEvent) {
        LOGGER.info(joystickEvent.toString());
        joystickEvents.fire(joystickEvent);
    }

}
