package io.freedriver.autonomy.async;

import io.freedriver.autonomy.config.Configuration;
import io.freedriver.autonomy.config.PinGroup;
import io.freedriver.autonomy.entity.event.EventType;
import io.freedriver.autonomy.entity.event.input.joystick.JoystickEvent;
import io.freedriver.autonomy.entity.event.input.joystick.JoystickEventType;
import io.freedriver.autonomy.service.ConnectorService;
import io.freedriver.jsonlink.Connector;
import io.freedriver.jsonlink.ConnectorException;
import io.freedriver.jsonlink.jackson.schema.v1.Identifier;
import io.freedriver.jsonlink.jackson.schema.v1.Request;
import io.freedriver.jsonlink.jackson.schema.v1.Response;

import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@ApplicationScoped
public class JoystickEventActor {

    private static final Logger LOGGER = Logger.getLogger(JoystickEventActor.class.getName());

    @Inject
    private Configuration configuration;

    @Inject @Any
    private Instance<Connector> connectors;

    @Inject
    private ConnectorService connectorService;

    @Resource
    private ManagedExecutorService pool;

    public void actOnEvent(@Observes @Default JoystickEvent joystickEvent) throws IOException {

        LOGGER.info("Observed: " + joystickEvent.toString());
        if (joystickEvent.getJoystickEventType() == JoystickEventType.BUTTON_UP && joystickEvent.getDescription().getType() != EventType.INITIAL_STATE) {
            String target = joystickEvent.getNumber().equals(11L) ?
                    "hallway" : "bathroom";
            try {
                connectorService.cyclePinGroup(target);
            } catch (ConnectorException e) {
                LOGGER.log(Level.WARNING, e, () -> "Couldn't act on Joystick Event.");
            }
        }
    }

}

