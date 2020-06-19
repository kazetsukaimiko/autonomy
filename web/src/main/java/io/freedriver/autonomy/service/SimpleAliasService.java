package io.freedriver.autonomy.service;

import io.freedriver.autonomy.Autonomy;
import io.freedriver.autonomy.cdi.qualifier.ConnectorCache;
import io.freedriver.autonomy.jpa.entity.event.input.joystick.JoystickEvent;
import io.freedriver.autonomy.rest.provider.ObjectMapperContextResolver;
import io.freedriver.autonomy.rest.view.AliasView;
import io.freedriver.base.util.file.DirectoryProviders;
import io.freedriver.jsonlink.config.v2.Appliance;
import io.freedriver.jsonlink.config.v2.Mapping;
import io.freedriver.jsonlink.config.v2.Mappings;
import io.freedriver.jsonlink.jackson.schema.v1.*;
import org.infinispan.Cache;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@ApplicationScoped
public class SimpleAliasService {
    private static final Logger LOGGER = Logger.getLogger(SimpleAliasService.class.getName());

    @Inject
    private ConnectorService connectorService;

    @Inject
    @ConnectorCache
    private Cache<PinCoordinate, Boolean> digitalPinCache;

    /**
     * Conversion from aliases to their mapped pin numbers for controller i/o.
     */
    public Map<Identifier, Boolean> identifiers(UUID boardId, Map<String, Boolean> desiredState) throws IOException {
        Map<String, Identifier> namedPins = getMapping(boardId)
                .getAppliances()
                .stream()
                .collect(Collectors.toMap(
                        Appliance::getName,
                        Appliance::getIdentifier,
                        (a, b) -> b
                ));

        return desiredState.keySet()
                .stream()
                .collect(Collectors.toMap(
                        namedPins::get,
                        desiredState::get,
                        (a, b) -> a
                ));
    }

    public Mapping getMapping(UUID boardId) throws IOException {
        return getMappings()
                .getMappings()
                .stream()
                .filter(mapping -> Objects.equals(boardId, mapping.getConnectorId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unable to find mapping for board " + boardId));
    }

    public Mappings getMappings() throws IOException {
        return ObjectMapperContextResolver.getMapper().readValue(
                DirectoryProviders.CONFIG
                        .getProvider()
                        .subdir(Autonomy.DEPLOYMENT)
                        .file("mappings_v2.json")
                        .get()
                        .toFile(),
                Mappings.class);
    }

    public Map<Identifier, Boolean> currentState(UUID boardId, Mapping mapping) {
        Map<Identifier, Boolean> fromCache = mapping.getAppliances().stream()
                .map(appliance -> new PinCoordinate(boardId, appliance.getIdentifier()))
                .filter(pinCoordinate -> digitalPinCache.containsKey(pinCoordinate))
                .collect(Collectors.toMap(
                        PinCoordinate::getIdentifier,
                        digitalPinCache::get,
                        (a, b) -> b
                ));
        if (fromCache.keySet().containsAll(mapping.getAppliances().stream().map(Appliance::getIdentifier)
                .collect(Collectors.toSet()))) {
            return fromCache;
        }
        return cacheBoardState(boardId, connectorService.readDigital(boardId, mapping.getAppliances()
                .stream().map(Appliance::getIdentifier).collect(Collectors.toSet())));
    }

    public Map<Identifier, Boolean> cacheBoardState(UUID boardId, Map<Identifier, Boolean> currentState) {
        currentState.forEach((k, v) -> digitalPinCache.put(new PinCoordinate(boardId, k), v));
        return currentState;
    }


    public AliasView newView(UUID boardId) throws IOException {
        return makeView(boardId);
        /*
        return oneSecondCache.computeIfAbsent(boardId, uuid -> {
            try {
                return makeView(uuid);
            } catch (IOException e) {
                throw new ViewCreationException("Problem creating view", e);
            }
        });*/
    }

    public AliasView makeView(UUID boardId) throws IOException {
        Mapping mapping = getMapping(boardId);
        AliasView aliasView = new AliasView();

        Map<Identifier, Boolean> actualState = currentState(boardId, mapping);
        aliasView.setApplianceStates(mapping.getAppliances()
                .stream()
                .filter(appliance -> actualState.containsKey(appliance.getIdentifier()))
                .collect(Collectors.toMap(
                        Appliance::getName,
                        appliance -> actualState.get(appliance.getIdentifier()),
                        (a, b) -> a
                )));

        Map<String, Set<Boolean>> grouped = mapping.getAppliances()
                .stream()
                .reduce(new HashMap<>(),
                        (hm, app) -> {
                            app.getGroups()
                                    .forEach(group -> {
                                        if (!hm.containsKey(group)) {
                                            hm.put(group, new HashSet<>());
                                        }
                                        hm.get(group).add(actualState.get(app.getIdentifier()));
                                    });
                            return hm;
                        },
                        (a, b) -> a);

        aliasView.setGroupStates(grouped.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> !e.getValue().contains(false)
                )));

        return aliasView;
    }

    public Map<Identifier, Boolean> setState(UUID boardId, Map<String, Boolean> desiredState) throws IOException {
        if (!desiredState.isEmpty()) {
            LOGGER.info("Setting states:");
            desiredState.forEach((k, v) -> LOGGER.info(k+": " + (v ? "true":"false")));
            return cacheBoardState(boardId, connectorService
                    .writeDigital(boardId, identifiers(boardId, desiredState)));
        }
        return Collections.emptyMap();
    }

    public Response setupBoard(UUID boardId) throws IOException {
        Request request = new Request();
        getMapping(boardId)
                .getAppliances()
                .stream()
                .map(Appliance::getIdentifier)
                .map(id -> new ModeSet(id, Mode.OUTPUT))
                .forEach(request::modeSet);
        return connectorService.send(boardId, request);
    }

    /**
     * On a genuine joystick event, search mappings for actions to commit to and execute them.
     * TODO: JoystickPressEvent using temporal data.
     * @param joystickEvent
     * @throws IOException
     */
    public void handleJoystickEvent(@Observes @Default JoystickEvent joystickEvent) throws IOException {
        if (!joystickEvent.isInitial() && joystickEvent.isButton()) {
            try {
                getMappings()
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
        Map<Identifier, Boolean> actualState = currentState(mapping.getConnectorId(), mapping);
        // Whether they should be turned on or not.
        boolean setStateAs = mapping.getAppliances()
                .stream()
                .filter(appliance -> appliances.contains(appliance.getName()))
                .noneMatch(appliance -> actualState.get(appliance.getIdentifier()));

        Request request = mapping.getAppliances()
                .stream()
                .filter(appliance -> appliances.contains(appliance.getName()))
                .reduce(
                        new Request(),
                        (req, app) -> req.digitalWrite(new DigitalWrite(app.getIdentifier(),
                                DigitalState.fromBoolean(setStateAs))), (a, b) -> a);

        LOGGER.finest(request.toString());

        cacheBoardState(mapping.getConnectorId(), connectorService.send(mapping.getConnectorId(), request).getDigital());
    }
}
