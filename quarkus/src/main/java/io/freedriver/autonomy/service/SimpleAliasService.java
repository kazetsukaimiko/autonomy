package io.freedriver.autonomy.service;

import io.freedriver.autonomy.Autonomy;
import io.freedriver.autonomy.cdi.qualifier.ConnectorCache;
import io.freedriver.autonomy.cdi.qualifier.SensorCache;
import io.freedriver.autonomy.jaxrs.ObjectMapperContextResolver;
import io.freedriver.autonomy.jaxrs.view.AliasView;
import io.freedriver.autonomy.jpa.entity.event.input.joystick.JoystickEvent;
import io.freedriver.base.util.file.DirectoryProviders;
import io.freedriver.jsonlink.config.v2.AnalogSensor;
import io.freedriver.jsonlink.config.v2.Appliance;
import io.freedriver.jsonlink.config.v2.Mapping;
import io.freedriver.jsonlink.config.v2.Mappings;
import io.freedriver.jsonlink.jackson.schema.v1.AnalogResponse;
import io.freedriver.jsonlink.jackson.schema.v1.DigitalState;
import io.freedriver.jsonlink.jackson.schema.v1.DigitalWrite;
import io.freedriver.jsonlink.jackson.schema.v1.Identifier;
import io.freedriver.jsonlink.jackson.schema.v1.Mode;
import io.freedriver.jsonlink.jackson.schema.v1.ModeSet;
import io.freedriver.jsonlink.jackson.schema.v1.Request;
import io.freedriver.jsonlink.jackson.schema.v1.Response;
import io.quarkus.runtime.StartupEvent;
import org.infinispan.Cache;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@ApplicationScoped
public class SimpleAliasService {
    private static final Logger LOGGER = Logger.getLogger(SimpleAliasService.class.getName());

    @Inject
    ConnectorService connectorService;

    @Inject
    @ConnectorCache
    Cache<PinCoordinate, Boolean> digitalPinCache;

    @Inject
    @SensorCache
    Cache<PinCoordinate, SensorValues> sensorCache;


    public void init(@Observes StartupEvent ev) {
        while (true) {
            try {
                getMappings()
                        .getMappings()
                        .forEach(this::cacheAnalogPins);
            } catch (IOException ioe) {
                LOGGER.log(Level.WARNING, "Couldn't cache Analog Pin State. ", ioe);
            }
        }
    }

    public void cacheAnalogPins(Mapping mapping) {
        try {
            Request readAnalogPinsAnyway = new Request()
                    .analogRead(mapping.getAnalogSensors().stream().map(AnalogSensor::asAnalogRead));
            cacheBoardState(mapping.getConnectorId(), connectorService.send(mapping.getConnectorId(), readAnalogPinsAnyway));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Couldn't cache Analog Pin State. ", e);
        }
    }

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
        Map<Identifier, Boolean> digitalStateFromCache = mapping.getAppliances().stream()
                .map(appliance -> new PinCoordinate(boardId, appliance.getIdentifier()))
                .filter(pinCoordinate -> digitalPinCache.containsKey(pinCoordinate))
                .collect(Collectors.toMap(
                        PinCoordinate::getIdentifier,
                        digitalPinCache::get,
                        (a, b) -> b
                ));
        if (digitalStateFromCache.keySet().containsAll(mapping.getAppliances().stream().map(Appliance::getIdentifier)
                .collect(Collectors.toSet()))) {
            return digitalStateFromCache;
        }
        return cacheBoardState(boardId, connectorService
                .readDigitalAndAnalog(
                        boardId,
                        mapping.getAppliances().stream().map(Appliance::getIdentifier).collect(Collectors.toSet()),
                        mapping.getAnalogSensors().stream().map(AnalogSensor::asAnalogRead))
        ).getDigital();
    }

    public Map<Identifier, Boolean> cacheBoardDigitalState(UUID boardId, Map<Identifier, Boolean> digitalState) {
        digitalState.forEach((k, v) -> digitalPinCache.put(new PinCoordinate(boardId, k), v));
        return digitalState;
    }

    public Response cacheBoardState(UUID boardId, Response currentState) {
        // Cache Digital Pins
        currentState.getDigital().forEach((k, v) ->
                digitalPinCache.put(new PinCoordinate(boardId, k), v));

        // Cache Analog Pins
        currentState.getAnalog()
                .forEach(analogResponse -> applyAnalogCache(boardId, analogResponse));

        // Cache Analog Pins
        currentState.getAnalog().forEach(analogResponse -> applyAnalogCache(boardId, analogResponse));

        return currentState;
    }

    private void applyAnalogCache(UUID boardId, AnalogResponse analogResponse) {
        PinCoordinate coordinate = new PinCoordinate(boardId, analogResponse.getPin());
        SensorValues values = new SensorValues();
        if (sensorCache.containsKey(coordinate)) {
            values = sensorCache.get(coordinate);
        }
        values.apply(analogResponse.getRaw());
        sensorCache.remove(coordinate);
        sensorCache.put(coordinate, values);
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

        Map<Identifier, Boolean> digitalState = currentState(boardId, mapping);
        aliasView.setApplianceStates(mapping.getAppliances()
                .stream()
                .filter(appliance -> digitalState.containsKey(appliance.getIdentifier()))
                .collect(Collectors.toMap(
                        Appliance::getName,
                        appliance -> digitalState.get(appliance.getIdentifier()),
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
                                        hm.get(group).add(digitalState.get(app.getIdentifier()));
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

        sensorCache.entrySet().stream()
                .filter(e -> Objects.equals(boardId, e.getKey().getBoardId()))
                .forEach(e -> getAnalogSensorByPin(mapping, e.getKey())
                        .ifPresent(analogSensor -> applySensorMetrics(aliasView, analogSensor, e.getValue())));

        return aliasView;
    }

    private void applySensorMetrics(AliasView view, AnalogSensor analogSensor, SensorValues sensorValues) {
        view.getSensors()
                .put(analogSensor.getName(), sensorValues.getRaw());
        view.getSensorMins()
                .put(analogSensor.getName(), sensorValues.getMin());
        view.getSensorMaxes()
                .put(analogSensor.getName(), sensorValues.getMax());
        view.getSensorPercentages()
                .put(analogSensor.getName(), sensorValues.getPercentage());
    }

    public Optional<AnalogSensor> getAnalogSensorByPin(Mapping mapping, PinCoordinate coordinate) {
        return mapping.getAnalogSensors()
                .stream()
                .filter(analogSensor -> Objects.equals(coordinate.getIdentifier(), analogSensor.getPin()))
                .findFirst();
    }

    public Response setState(UUID boardId, Map<String, Boolean> desiredState) throws IOException {
        Mapping mapping = getMapping(boardId);
        if (!desiredState.isEmpty()) {
            LOGGER.info("Setting states:");
            desiredState.forEach((k, v) -> LOGGER.info(k+": " + (v ? "true":"false")));

            // TODO: Remove
            //return cacheBoardDigitalState(boardId, connectorService
            //        .writeDigital(boardId, identifiers(boardId, desiredState)));

            // NEW
            Request r = new Request();
            identifiers(boardId, desiredState)
                    .entrySet()
                    .stream()
                    .map(e -> new DigitalWrite(e.getKey(), e.getValue()))
                    .forEach(r::digitalWrite);
            mapping.getAnalogSensors().stream().map(AnalogSensor::asAnalogRead)
                    .forEach(r::analogRead);

            return cacheBoardState(boardId, connectorService.send(boardId, r));
        }
        return new Response();
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
        Map<Identifier, Boolean> digitalState = currentState(mapping.getConnectorId(), mapping);
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
        // Read Analog pins
        request.analogRead(mapping.getAnalogSensors().stream().map(AnalogSensor::asAnalogRead));

        LOGGER.finest(request.toString());

        cacheBoardState(mapping.getConnectorId(), connectorService.send(mapping.getConnectorId(), request));
        //cacheBoardDigitalState(mapping.getConnectorId(), connectorService.send(mapping.getConnectorId(), request).getDigital());
    }
}
