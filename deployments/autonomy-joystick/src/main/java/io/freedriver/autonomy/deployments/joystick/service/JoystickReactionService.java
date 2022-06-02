package io.freedriver.autonomy.deployments.joystick.service;

import io.freedriver.autonomy.event.input.joystick.jstest.AllJoysticks;
import io.freedriver.autonomy.jpa.entity.event.input.joystick.JoystickEvent;
import io.freedriver.autonomy.jpa.entity.event.input.joystick.jstest.JSTestEvent;
import io.freedriver.jsonlink.config.v2.Mapping;
import io.freedriver.jsonlink.jackson.schema.v1.DigitalState;
import io.freedriver.jsonlink.jackson.schema.v1.DigitalWrite;
import io.freedriver.jsonlink.jackson.schema.v1.Identifier;
import io.freedriver.jsonlink.jackson.schema.v1.Request;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class JoystickReactionService {

    private static final Logger LOGGER = Logger.getLogger(JoystickReactionService.class.getName());

    @Inject
    SimpleAliasService simpleAliasService;

    /**
     * On a genuine joystick event, search mappings for actions to commit to and execute them.
     * TODO: JoystickPressEvent using temporal data.
     * @param joystickEvent
     * @throws IOException
     */
    public void handleJoystickEvent(@Observes @Default JoystickEvent joystickEvent) throws IOException {
        if (!joystickEvent.isInitial() && joystickEvent.getJoystickEventType().isButton()) {
            try {
                simpleAliasService.getMappings()
                        .getMappings()
                        .forEach(mapping -> handleJoystickEvent(joystickEvent, mapping));
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to observe JoystickEvent: ", e);
            }
        }
    }

    public void handleJoystickEvent(JoystickEvent joystickEvent, Mapping mapping) {
        String eventId = joystickEvent.getNumber() + ":" + joystickEvent.getValue();
        Optional.of(eventId)
                .map(mapping.getControlMap()::get)
                .ifPresent(appliances -> toggleAppliances(mapping, appliances));
    }

    private void toggleAppliances(Mapping mapping, List<String> appliances) {
        LOGGER.info("Toggling appliances from joystick input: " + String.join(", ", appliances));
        Map<Identifier, Boolean> digitalState = simpleAliasService.currentState(mapping.getConnectorId(), mapping);
        // Whether they should be turned on or not.
        boolean setStateAs = mapping.getAppliances()
                .stream()
                .filter(appliance -> appliances.contains(appliance.getName()))
                .noneMatch(appliance -> digitalState.get(appliance.getIdentifier()));

        // Read appliances
        Request request = mapping.getAppliances()
                .stream()
                .filter(appliance -> appliances.contains(appliance.getName()))
                .reduce(
                        new Request(),
                        (req, app) -> req.digitalWrite(new DigitalWrite(app.getIdentifier(),
                                DigitalState.fromBoolean(setStateAs))), (a, b) -> a);

        LOGGER.finest(request.toString());

        simpleAliasService.cacheBoardState(mapping, request);
    }


    private AllJoysticks allJoysticks;

    private final ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()*10);

    @Inject
    Event<JoystickEvent> joystickEvents;


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


}
