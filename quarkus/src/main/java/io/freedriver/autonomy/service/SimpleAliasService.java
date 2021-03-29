package io.freedriver.autonomy.service;

import io.freedriver.autonomy.Autonomy;
import io.freedriver.autonomy.cdi.qualifier.ConnectorCache;
import io.freedriver.autonomy.cdi.qualifier.SensorCache;
import io.freedriver.autonomy.jaxrs.ObjectMapperContextResolver;
import io.freedriver.autonomy.jaxrs.view.AliasView;
import io.freedriver.autonomy.jpa.entity.event.GenerationOrigin;
import io.freedriver.autonomy.jpa.entity.event.input.joystick.JoystickEvent;
import io.freedriver.autonomy.jpa.entity.event.input.sensors.FloatValueSensorEvent;
import io.freedriver.autonomy.jpa.entity.event.speech.SpeechEvent;
import io.freedriver.autonomy.jpa.entity.event.speech.SpeechEventType;
import io.freedriver.autonomy.service.crud.EventCrudService;
import io.freedriver.base.util.file.DirectoryProviders;
import io.freedriver.jsonlink.config.v2.AnalogAlert;
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
import liquibase.pro.packaged.I;
import org.infinispan.Cache;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class SimpleAliasService  {
    private static final Logger LOGGER = Logger.getLogger(SimpleAliasService.class.getName());

    private static Path HISTORY_FILE = DirectoryProviders.CONFIG
            .getProvider()
            .subdir(Autonomy.DEPLOYMENT)
            .file("sensor_history.json")
            .get();

    // Arbitrary. TODO: Rethink.
    private ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()*10);

    @Inject
    ConnectorService connectorService;

    @Inject
    FloatValueSensorEventService floatValueSensorService;

    @Inject
    @ConnectorCache
    Cache<PinCoordinate, Boolean> digitalPinCache;

    @Inject
    @SensorCache
    Cache<PinCoordinate, SensorValues> sensorCache;

    @Inject
    Event<SpeechEvent> speech;

    public void waitFor(Duration duration) throws InterruptedException {
        Thread.sleep(duration.toMillis());
    }

    public void populateSensorCacheFromHistory() {
        SensorHistory sensorHistory = readSensorHistory();
        sensorHistory.getHistory()
                .forEach(this::populateBoardHistory);
    }

    public void populateBoardHistory(UUID boardId, BoardAnalogHistory boardAnalogHistory) {
        Stream.concat(boardAnalogHistory.getMaximums().keySet().stream(), boardAnalogHistory.getMinimums().keySet().stream())
                .collect(Collectors.toSet())
                .stream()
                .map(identifier -> new PinCoordinate(boardId, identifier))
                .forEach(coordinate -> populatePinHistory(coordinate, boardAnalogHistory));
    }

    private void populatePinHistory(PinCoordinate coordinate, BoardAnalogHistory boardAnalogHistory) {
        SensorValues sensorValues = new SensorValues();
        if (boardAnalogHistory.getMinimums().containsKey(coordinate.getIdentifier())) {
            sensorValues.setMin(boardAnalogHistory.getMinimums().get(coordinate.getIdentifier()));
        }
        if (boardAnalogHistory.getMaximums().containsKey(coordinate.getIdentifier())) {
            sensorValues.setMax(boardAnalogHistory.getMaximums().get(coordinate.getIdentifier()));
        }
        if (boardAnalogHistory.getLastKnowns().containsKey(coordinate.getIdentifier())) {
            sensorValues.setRaw(boardAnalogHistory.getLastKnowns().get(coordinate.getIdentifier()));
        }
        sensorCache.put(coordinate, sensorValues);
    }

    public void refreshAnalogPins() {
        populateSensorCacheFromHistory();
        while (true) {
            try {
                List<Future<Boolean>> requests = getMappings()
                        .getMappings()
                        .stream()
                        .map(this::cacheAnalogPins)
                        .collect(Collectors.toList());
                while(!requests.stream().allMatch(Future::isDone)) {
                    waitFor(Duration.ofMillis(1));
                }
                waitFor(Duration.ofMillis(500));
            } catch (IOException | InterruptedException e) {
                LOGGER.log(Level.WARNING, "Couldn't cache Analog Pin State. ", e);
            }
        }
    }

    public Future<Boolean> cacheAnalogPins(Mapping mapping) {
        return pool.submit(() -> {
            try {
                Request readAnalogPinsAnyway = new Request()
                        .analogRead(mapping.getAnalogSensors().stream().map(AnalogSensor::asAnalogRead));
                Response response = connectorService.send(mapping.getConnectorId(), readAnalogPinsAnyway);
                cacheBoardState(mapping, response);
                sendAnalogSensorEvents(mapping, response);
                return true;
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Couldn't cache Analog Pin State. ", e);
                return false;
            }
        });
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
        return cacheBoardState(mapping, connectorService
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

    public Response cacheBoardState(Mapping mapping, Response currentState) {
        // Cache Digital Pins
        currentState.getDigital().forEach((k, v) ->
                digitalPinCache.put(new PinCoordinate(mapping.getConnectorId(), k), v));

        // Cache Analog Pins
        currentState.getAnalog()
                .forEach(analogResponse -> applyAnalogCache(mapping.getConnectorId(), analogResponse));

        persistAnalogCache();

        Map<String, Float> percentages = mapping.getAnalogSensors()
            .stream()
            .filter(analogSensor -> sensorCache.keySet()
                .stream()
                .anyMatch(coordinate -> Objects.equals(coordinate, new PinCoordinate(mapping.getConnectorId(), analogSensor.getPin()))))
            .collect(Collectors.toMap(
                    AnalogSensor::getName,
                    as -> sensorCache.get(new PinCoordinate(mapping.getConnectorId(), as.getPin())).getPercentage(),
                    (a, b) -> b
            ));

        // Fire Alert events as needed
        mapping.getAnalogAlerts()
                .forEach(analogAlert -> {
                    if (percentages.keySet().containsAll(analogAlert.getSensors())) {
                        if (analogAlert.getMatching().test(
                                analogAlert.getValue(),
                                analogAlert.getCondition(),
                                percentages.entrySet().stream()
                                    .filter(e -> analogAlert.getSensors().stream()
                                        .anyMatch(name -> Objects.equals(name, e.getKey())))
                                        .map(Map.Entry::getValue))) {
                            speak(analogAlert, percentages);
                        }
                    } else {
                        LOGGER.warning("Couldn't process AnalogAlert- missing mappings. " + analogAlert);
                    }
                });

        return currentState;
    }

    private void speak(AnalogAlert analogAlert, Map<String, Float> percentages) {
        LOGGER.info("AnalogAlert qualified: " + analogAlert);
        LOGGER.info("Percentages: \n" + percentages.entrySet()
                .stream()
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining("\n")));
        SpeechEvent speechEvent = new SpeechEvent();
        speechEvent.setSourceId("sensors://"+String.join(",", analogAlert.getSensors()));
        speechEvent.setSourceClass(getClass().getName());
        speechEvent.setSubject(String.join("/", analogAlert.getSensors()));
        speechEvent.setSpeechEventType(SpeechEventType.INFO);
        speechEvent.setText(analogAlert.getContent());
        speech.fire(speechEvent);
    }

    private void sendAnalogSensorEvents(Mapping mapping, Response currentState) {
        currentState.getAnalog()
                .forEach(analogResponse -> getAnalogSensorByMapping(mapping, analogResponse)
                    .ifPresent(analogSensor -> {
                        FloatValueSensorEvent event = new FloatValueSensorEvent();
                        event.setBoardId(mapping.getConnectorId());
                        event.setSensorName(mapping.getConnectorId() + "/" +analogSensor.getName()+"/live");
                        event.setGenerationOrigin(GenerationOrigin.NON_HUMAN);
                        event.setSourceId(mapping.getConnectorId().toString());
                        event.setSourceClass(getClass().getName());
                        event.setSourceId(mapping.getConnectorId().toString());
                        event.setEventId("analog/"+analogSensor.getPin().getPin());
                        event.setValue(analogResponse.getRaw());
                        floatValueSensorService.save(event);
                }));
    }

    private Optional<AnalogSensor> getAnalogSensorByMapping(Mapping mapping, AnalogResponse analogResponse) {
        return mapping.getAnalogSensors()
                .stream()
                .filter(analogSensor -> Objects.equals(analogSensor.getPin(), analogResponse.getPin()))
                .findFirst();
    }

    private synchronized SensorHistory readSensorHistory() {
        SensorHistory sensorHistory = new SensorHistory();
        try {
            if (Files.exists(HISTORY_FILE) && Files.isReadable(HISTORY_FILE)) {
                sensorHistory = ObjectMapperContextResolver.getMapper().readValue(
                        HISTORY_FILE.toFile(),
                        SensorHistory.class
                );
            }
        } catch (IOException ioe) {
            LOGGER.log(Level.WARNING, "Couldn't read sensor history file", ioe);
        }
        return sensorHistory;
    }

    private synchronized void writeSensorHistory(SensorHistory sensorHistory) {
        try {
            ObjectMapperContextResolver.getMapper().writeValue(HISTORY_FILE.toFile(), sensorHistory);
        } catch (IOException ioe) {
            LOGGER.log(Level.WARNING, "Couldn't write sensor history file", ioe);
        }
    }

    private synchronized void persistAnalogCache() {
        SensorHistory sensorHistory = readSensorHistory();
        sensorCache
                .forEach((key, value) -> {
                    if (!sensorHistory.getHistory().containsKey(key.getBoardId())) {
                        sensorHistory.getHistory().put(key.getBoardId(), new BoardAnalogHistory());
                    }
                    BoardAnalogHistory boardHistory = sensorHistory.getHistory().get(key.getBoardId());
                    boardHistory.getMinimums()
                            .put(key.getIdentifier(), value.getMin());
                    boardHistory.getMaximums()
                            .put(key.getIdentifier(), value.getMax());
                    boardHistory.getLastKnowns()
                            .put(key.getIdentifier(), value.getRaw());
                });

        writeSensorHistory(sensorHistory);
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


        Map<String, String> sensorMinMaxes = Stream.concat(aliasView.getSensorMins().keySet().stream(), aliasView.getSensorMaxes().keySet().stream())
                .collect(Collectors.toSet())
                .stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        key -> key + ": " +aliasView.getSensorMins().get(key)+ "-" + aliasView.getSensorMaxes().get(key) + "; " + aliasView.getSensors().get(key),
                        (a, b) -> a));

        String sensorPercentages = aliasView.getSensorPercentages()
                .entrySet()
                .stream()
                .map(e -> e.getKey() + ": " + e.getValue() + " values " + sensorMinMaxes.get(e.getKey()))
                .collect(Collectors.joining("\n"));


        //LOGGER.info("Sensor percentages; \n" + sensorPercentages);

        return aliasView;
    }

    private void applySensorMetrics(AliasView view, AnalogSensor analogSensor, SensorValues sensorValues) {
        view.getSensors()
                .put(analogSensor.getName(), scaleSensor(analogSensor, sensorValues.getRaw()));
        view.getSensorMins()
                .put(analogSensor.getName(), scaleSensor(analogSensor, sensorValues.getMin()));
        view.getSensorMaxes()
                .put(analogSensor.getName(), scaleSensor(analogSensor, sensorValues.getMax()));
        view.getSensorPercentages()
                .put(analogSensor.getName(), getSensorPercentage(
                        view.getSensorMins().get(analogSensor.getName()),
                        view.getSensorMaxes().get(analogSensor.getName()),
                        view.getSensors().get(analogSensor.getName()),
                        analogSensor));
    }

    public float getSensorPercentage(float min, float max, float current, AnalogSensor analogSensor) {
        float percentage = BigDecimal.valueOf((current - min) / (max - min))
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP)
                .floatValue();
        return analogSensor.isInverted()
                ? 100F - percentage
                : percentage;
    }

    public float scaleSensor(AnalogSensor analogSensor, int sensorValue) {
        return sensorValue;

        //float v1 = sensorValue * (analogSensor.getVoltage() / 1023f);
        //return (analogSensor.getVoltage() - v1) * (analogSensor.getResistance() / v1);
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

            Response response = connectorService.send(boardId, r);
            sendAnalogSensorEvents(mapping, response);
            return cacheBoardState(mapping, response);
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

        cacheBoardState(mapping, connectorService.send(mapping.getConnectorId(), request));
        //cacheBoardDigitalState(mapping.getConnectorId(), connectorService.send(mapping.getConnectorId(), request).getDigital());
    }

}
