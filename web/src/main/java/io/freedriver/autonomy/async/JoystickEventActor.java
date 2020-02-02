package io.freedriver.autonomy.async;

import io.freedriver.autonomy.entity.event.StateType;
import io.freedriver.autonomy.entity.event.input.joystick.JoystickEvent;
import io.freedriver.autonomy.entity.event.input.joystick.JoystickEventType;
import io.freedriver.autonomy.service.ConnectorService;
import io.freedriver.jsonlink.Connector;
import io.freedriver.jsonlink.ConnectorException;
import io.freedriver.jsonlink.config.ConnectorConfig;

import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.io.IOException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class JoystickEventActor {

    private static final Logger LOGGER = Logger.getLogger(JoystickEventActor.class.getName());

    @Inject
    private ConnectorConfig configuration;

    @Inject @Any
    private Instance<Connector> connectors;

    @Inject
    private ConnectorService connectorService;

    @Resource
    private ManagedExecutorService pool;

    public void actOnEvent(@Observes @Default JoystickEvent joystickEvent) throws IOException {
        LOGGER.info("Observed: " + joystickEvent.toString());
        if (joystickEvent.getJoystickEventType() == JoystickEventType.BUTTON_UP && joystickEvent.getDescription().getType() != StateType.INITIAL_STATE) {
            String target = joystickEvent.getNumber().equals(11L) ?
                    "hallway" : "bathroom";
            try {
                connectorService.getWorkspace()
                        .getBoards().forEach(boardEntity -> boardEntity.getGroups().stream()
                                    .filter(groupEntity -> Objects.equals(target, groupEntity.getName()))
                                    .forEach(matchedGroup -> connectorService.nextPermutation(boardEntity.getBoardId(), matchedGroup)));
            } catch (ConnectorException | IllegalArgumentException e) {
                LOGGER.log(Level.WARNING, e, () -> "Couldn't act on Joystick Event.");
            }
        }
    }

}

