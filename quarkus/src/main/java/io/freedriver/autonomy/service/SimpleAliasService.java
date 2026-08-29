package io.freedriver.autonomy.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.freedriver.autonomy.Autonomy;
import io.freedriver.autonomy.cdi.qualifier.ConnectorCache;
import io.freedriver.autonomy.cdi.qualifier.SensorCache;
import io.freedriver.autonomy.event.GenerationOrigin;
import io.freedriver.autonomy.event.input.joystick.JoystickEvent;
import io.freedriver.autonomy.event.input.sensors.DoubleValueSensorEvent;
import io.freedriver.autonomy.event.speech.SpeechEvent;
import io.freedriver.autonomy.event.speech.SpeechEventType;
import io.freedriver.autonomy.jaxrs.ObjectMapperContextResolver;
import io.freedriver.autonomy.jaxrs.view.AliasView;
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
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class SimpleAliasService  {

    private static Path HISTORY_FILE = DirectoryProviders.CONFIG
            .getProvider()
            .subdir(Autonomy.DEPLOYMENT)
            .file("sensor_history.json")
            .get();

    // Arbitrary. TODO: Rethink.
    private ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()*10);

    private final Map<AnalogSensor, List<SensorValues>> sensorAverages = new HashMap<>();

    @Inject
    ConnectorService connectorService;

    @Inject
    FloatValueSensorEventService floatValueSensorService;

    @Inject
    @ConnectorCache
    Map<PinCoordinate, Boolean> digitalPinCache;

    @Inject
    @SensorCache
    Map<PinCoordinate, SensorValues> sensorCache;

    @Inject
    Event<SpeechEvent> speech;

    public void waitFor(Duration duration) throws InterruptedException {
        Thread.sleep(duration.toMillis());
    }

    public void populateSensorCacheFromHistory() {
        SensorHistory sensorHistory = readSensorHistory();
        sensorHistory.history()
                .forEach(this::populateBoardHistory);
    }

    public void populateBoardHistory(UUID boardId, BoardAnalogHistory boardAnalogHistory) {
        Stream.concat(boardAnalogHistory.maximums().keySet().stream(), boardAnalogHistory.minimums().keySet().stream())
                .collect(Collectors.toSet())
                .stream()
                .map(identifier -> new PinCoordinate(boardId, identifier))
                .forEach(coordinate -> populatePinHistory(coordinate, boardAnalogHistory));
    }

    private void populatePinHistory(PinCoordinate coordinate, BoardAnalogHistory boardAnalogHistory) {
        SensorValues sensorValues = new SensorValues();
        if (boardAnalogHistory.minimums().containsKey(coordinate.identifier())) {
            sensorValues = sensorValues.toBuilder().min(boardAnalogHistory.minimums().get(coordinate.identifier())).build();
        }
        if (boardAnalogHistory.maximums().containsKey(coordinate.identifier())) {
            sensorValues = sensorValues.toBuilder().max(boardAnalogHistory.maximums().get(coordinate.identifier())).build();
        }
        if (boardAnalogHistory.lastKnowns().containsKey(coordinate.identifier())) {
            sensorValues = sensorValues.toBuilder().raw(boardAnalogHistory.lastKnowns().get(coordinate.identifier())).build();
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
                log.warn("Couldn't cache Analog Pin State. ", e);
            }
        }
    }

    public Future<Boolean> cacheAnalogPins(Mapping mapping) {
        return pool.submit(() -> {
            try {
                Request readAnalogPinsAnyway = Request.empty()
                        .analogRead(mapping.analogSensors().stream().map(AnalogSensor::asAnalogRead));
                Response response = connectorService.send(mapping.connectorId(), readAnalogPinsAnyway);
                cacheBoardState(mapping, response);
                sendAnalogSensorEvents(mapping, response);
                return true;
            } catch (Exception e) {
                log.warn("Couldn't cache Analog Pin State. ", e);
                return false;
            }
        });
    }

    /**
     * Conversion from aliases to their mapped pin numbers for controller i/o.
     */
    public Map<Identifier, Boolean> identifiers(UUID boardId, Map<String, Boolean> desiredState) throws IOException {
        Map<String, Identifier> namedPins = getMapping(boardId)
                .appliances()
                .stream()
                .collect(Collectors.toMap(
                        Appliance::name,
                        Appliance::identifier,
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
                .filter(mapping -> Objects.equals(boardId, mapping.connectorId()))
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
        Map<Identifier, Boolean> digitalStateFromCache = mapping.appliances().stream()
                .map(appliance -> new PinCoordinate(boardId, appliance.identifier()))
                .filter(pinCoordinate -> digitalPinCache.containsKey(pinCoordinate))
                .collect(Collectors.toMap(
                        PinCoordinate::identifier,
                        digitalPinCache::get,
                        (a, b) -> b
                ));
        if (digitalStateFromCache.keySet().containsAll(mapping.appliances().stream().map(Appliance::identifier)
                .collect(Collectors.toSet()))) {
            return digitalStateFromCache;
        }
        return cacheBoardState(mapping, connectorService
                .readDigitalAndAnalog(
                        boardId,
                        mapping.appliances().stream().map(Appliance::identifier).collect(Collectors.toSet()),
                        mapping.analogSensors().stream().map(AnalogSensor::asAnalogRead))
        ).digital();
    }

    public Map<Identifier, Boolean> cacheBoardDigitalState(UUID boardId, Map<Identifier, Boolean> digitalState) {
        digitalState.forEach((k, v) -> digitalPinCache.put(new PinCoordinate(boardId, k), v));
        return digitalState;
    }

    public Response cacheBoardState(Mapping mapping, Response currentState) {
        // Cache Digital Pins
        currentState.digital().forEach((k, v) ->
                digitalPinCache.put(new PinCoordinate(mapping.connectorId(), k), v));

        /*
        // Cache Analog Pins
        currentState.analog()
                .forEach(analogResponse -> applyAnalogCache(mapping.connectorId(), analogResponse));

        persistAnalogCache();

        Map<String, Double> percentages = mapping.analogSensors()
            .stream()
            .filter(analogSensor -> sensorCache.keySet()
                .stream()
                .anyMatch(coordinate -> Objects.equals(coordinate, new PinCoordinate(mapping.connectorId(), analogSensor.pin()))))
            .collect(Collectors.toMap(
                    AnalogSensor::name,
                    as -> averageSensorMetrics(as, sensorCache.get(new PinCoordinate(mapping.connectorId(), as.pin()))).percentage(),
                    (a, b) -> b
            ));

        // Fire Alert events as needed
        mapping.analogAlerts()
                .forEach(analogAlert -> {
                    if (percentages.keySet().containsAll(analogAlert.sensors())) {
                        if (analogAlert.matching().test(
                                analogAlert.value(),
                                analogAlert.condition(),
                                percentages.entrySet().stream()
                                    .filter(e -> analogAlert.sensors().stream()
                                        .anyMatch(name -> Objects.equals(name, e.getKey())))
                                        .map(Map.Entry::getValue))) {
                            speak(analogAlert, percentages);
                        }
                    } else {
                        log.warn("Couldn't process AnalogAlert- missing mappings. " + analogAlert);
                    }
                });

         */

        return currentState;
    }

    private void speak(AnalogAlert analogAlert, Map<String, Double> percentages) {
        log.info("AnalogAlert qualified: " + analogAlert);
        log.info("Percentages: \n" + percentages.entrySet()
                .stream()
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining("\n")));
        speech.fire(new SpeechEvent(
                java.util.UUID.randomUUID().toString(),
                Instant.now(),
                GenerationOrigin.NON_HUMAN,
                getClass().getName(),
                "sensors://" + String.join(",", analogAlert.sensors()),
                null,
                SpeechEventType.INFO,
                String.join("/", analogAlert.sensors()),
                analogAlert.content()));
    }

    private void sendAnalogSensorEvents(Mapping mapping, Response currentState) {
        currentState.analog()
                .forEach(analogResponse -> getAnalogSensorByMapping(mapping, analogResponse)
                    .ifPresent(analogSensor -> {
                        floatValueSensorService.save(new DoubleValueSensorEvent(
                                java.util.UUID.randomUUID().toString(),
                                Instant.now(),
                                GenerationOrigin.NON_HUMAN,
                                getClass().getName(),
                                mapping.connectorId().toString(),
                                "analog/" + analogSensor.pin().pin(),
                                mapping.connectorId(),
                                mapping.connectorId() + "/" + analogSensor.name() + "/live",
                                analogResponse.raw()));
                }));
    }

    private Optional<AnalogSensor> getAnalogSensorByMapping(Mapping mapping, AnalogResponse analogResponse) {
        return mapping.analogSensors()
                .stream()
                .filter(analogSensor -> Objects.equals(analogSensor.pin(), analogResponse.pin()))
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
            log.warn("Couldn't read sensor history file", ioe);
        }
        return sensorHistory;
    }

    private synchronized void writeSensorHistory(SensorHistory sensorHistory) {
        try {
            ObjectMapperContextResolver.getMapper().writeValue(HISTORY_FILE.toFile(), sensorHistory);
        } catch (IOException ioe) {
            log.warn("Couldn't write sensor history file", ioe);
        }
    }

    private synchronized void persistAnalogCache() {
        SensorHistory sensorHistory = readSensorHistory();
        sensorCache
                .forEach((key, value) -> {
                    if (!sensorHistory.history().containsKey(key.boardId())) {
                        sensorHistory.history().put(key.boardId(), new BoardAnalogHistory());
                    }
                    BoardAnalogHistory boardHistory = sensorHistory.history().get(key.boardId());
                    boardHistory.minimums()
                            .put(key.identifier(), value.min());
                    boardHistory.maximums()
                            .put(key.identifier(), value.max());
                    boardHistory.lastKnowns()
                            .put(key.identifier(), value.raw());
                });

        writeSensorHistory(sensorHistory);
    }

    private void applyAnalogCache(UUID boardId, AnalogResponse analogResponse) {
        PinCoordinate coordinate = new PinCoordinate(boardId, analogResponse.pin());
        SensorValues values = new SensorValues();
        if (sensorCache.containsKey(coordinate)) {
            values = sensorCache.get(coordinate);
        }
        values = values.apply(analogResponse.raw());
        sensorCache.remove(coordinate);
        sensorCache.put(coordinate, values);
    }

    public AliasView makeView(UUID boardId) throws IOException {
        Mapping mapping = getMapping(boardId);

        Map<Identifier, Boolean> digitalState = currentState(boardId, mapping);
        Map<String, Boolean> applianceStates = mapping.appliances()
                .stream()
                .filter(appliance -> digitalState.containsKey(appliance.identifier()))
                .collect(Collectors.toMap(
                        Appliance::name,
                        appliance -> digitalState.get(appliance.identifier()),
                        (a, b) -> a
                ));

        Map<String, Set<Boolean>> grouped = mapping.appliances()
                .stream()
                .reduce(new HashMap<>(),
                        (hm, app) -> {
                            app.groups()
                                    .forEach(group -> {
                                        if (!hm.containsKey(group)) {
                                            hm.put(group, new HashSet<>());
                                        }
                                        hm.get(group).add(digitalState.get(app.identifier()));
                                    });
                            return hm;
                        },
                        (a, b) -> a);

        Map<String, Boolean> groupStates = grouped.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> !e.getValue().contains(false)
                ));

        /*
        sensorCache.entrySet().stream()
                .filter(e -> Objects.equals(boardId, e.getKey().boardId()))
                .forEach(e -> getAnalogSensorByPin(mapping, e.getKey())
                        .ifPresent(analogSensor -> {
                            applySensorMetrics(aliasView, analogSensor, averageSensorMetrics(analogSensor, e.getValue()));
                        }));

         */

        return AliasView.of(applianceStates, groupStates);
    }


    private synchronized SensorValues averageSensorMetrics(AnalogSensor analogSensor, SensorValues sensorValues) {
        if (analogSensor.averageOver() < 0) {
            return sensorValues;
        }
        if (!sensorAverages.containsKey(analogSensor)) {
            sensorAverages.put(analogSensor, new ArrayList<>());
        } else {
            Instant expiresOn = Instant.now().plus(Duration.ofMillis(analogSensor.averageOver()));
            sensorAverages.get(analogSensor)
                    .removeIf(historical -> historical.recordedOn().isAfter(expiresOn));
        }
        sensorAverages.get(analogSensor)
                .add(sensorValues);

        SensorValues latest = sensorAverages.get(analogSensor)
                .stream()
                .max(Comparator.comparing(SensorValues::recordedOn))
                .orElse(sensorValues);

        int average = Double.valueOf(sensorAverages.get(analogSensor)
                .stream()
                .mapToInt(SensorValues::raw)
                .average()
                .orElse(latest.raw()))
                .intValue();

        return new SensorValues()
                .toBuilder()
                .recordedOn(sensorValues.recordedOn())
                .min(latest.min())
                .max(latest.max())
                .raw(average)
                .build()
                .apply(average);
    }



    /*
    private void applySensorMetrics(AliasView view, AnalogSensor analogSensor, SensorValues averagePacket) {
        view.sensors()
                .put(analogSensor.name(), Integer.valueOf(averagePacket.raw()).doubleValue());
        view.sensorMins()
                .put(analogSensor.name(), Integer.valueOf(averagePacket.min()).doubleValue());
        view.sensorMaxes()
                .put(analogSensor.name(), Integer.valueOf(averagePacket.max()).doubleValue());
        view.sensorPercentages()
                .put(analogSensor.name(),
                            getSensorPercentage(
                                    averagePacket.min(),
                                averagePacket.max(),
                                averagePacket.raw(),
                                analogSensor));
    }


    public double getSensorPercentage(double min, double max, double current, AnalogSensor analogSensor) {
        double percentage = BigDecimal.valueOf((current - min) / (max - min))
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();

        double base = (analogSensor.factor() != null && analogSensor.mode() != null)
                ? analogSensor.mode().realPercent(percentage, analogSensor.factor())
                : percentage;

        return analogSensor.inverted()
                ? 100F - base
                : base;
    }

    public double scaleSensor(AnalogSensor analogSensor, int sensorValue) {
        return sensorValue;

        //double v1 = sensorValue * (analogSensor.voltage() / 1023f);
        //return (analogSensor.voltage() - v1) * (analogSensor.resistance() / v1);
    }


    public Optional<AnalogSensor> getAnalogSensorByPin(Mapping mapping, PinCoordinate coordinate) {
        return mapping.analogSensors()
                .stream()
                .filter(analogSensor -> Objects.equals(coordinate.identifier(), analogSensor.pin()))
                .findFirst();
    }

     */

    public Response setState(UUID boardId, Map<String, Boolean> desiredState) throws IOException {
        Mapping mapping = getMapping(boardId);
        if (!desiredState.isEmpty()) {
            log.info("Setting states:");
            desiredState.forEach((k, v) -> log.info(k+": " + (v ? "true":"false")));

            Request r = identifiers(boardId, desiredState)
                    .entrySet()
                    .stream()
                    .map(e -> new DigitalWrite(e.getKey(), e.getValue()))
                    .reduce(Request.empty(), Request::digitalWrite, (a, b) -> a);
            r = mapping.analogSensors().stream().map(AnalogSensor::asAnalogRead)
                    .reduce(r, Request::analogRead, (a, b) -> a);

            Response response = connectorService.send(boardId, r);
            sendAnalogSensorEvents(mapping, response);
            return cacheBoardState(mapping, response);
        }
        return Response.empty();
    }

    public Response setupBoard(UUID boardId) throws IOException {
        Request request = getMapping(boardId)
                .appliances()
                .stream()
                .map(Appliance::identifier)
                .map(id -> new ModeSet(id, Mode.OUTPUT))
                .reduce(Request.empty(), Request::modeSet, (a, b) -> a);
        return connectorService.send(boardId, request);
    }

    /**
     * On a genuine joystick event, search mappings for actions to commit to and execute them.
     * TODO: JoystickPressEvent using temporal data.
     * @param joystickEvent
     * @throws IOException
     */
    public void handleJoystickEvent(@Observes @Default JoystickEvent joystickEvent) throws IOException {
        if (!joystickEvent.initial()
                && joystickEvent.joystickEventType() != null
                && joystickEvent.joystickEventType().isButton()) {
            try {
                getMappings()
                        .getMappings()
                        .forEach(mapping -> handleJoystickEvent(joystickEvent, mapping));
            } catch (Exception e) {
                log.warn("Failed to observe JoystickEvent: ", e);
            }
        }
    }

    public void handleJoystickEvent(JoystickEvent joystickEvent, Mapping mapping) {
        String eventId = joystickEvent.number() + ":" + joystickEvent.eventValue();
        Optional.of(eventId)
                .map(mapping.controlMap()::get)
                .ifPresent(appliances -> toggleAppliances(mapping, appliances));
    }

    private void toggleAppliances(Mapping mapping, List<String> appliances) {
        Map<Identifier, Boolean> digitalState = currentState(mapping.connectorId(), mapping);
        // Whether they should be turned on or not.
        boolean setStateAs = mapping.appliances()
                .stream()
                .filter(appliance -> appliances.contains(appliance.name()))
                .noneMatch(appliance -> digitalState.get(appliance.identifier()));

        // Read appliances
        Request request = mapping.appliances()
                .stream()
                .filter(appliance -> appliances.contains(appliance.name()))
                .reduce(
                        Request.empty(),
                        (req, app) -> req.digitalWrite(new DigitalWrite(app.identifier(),
                                DigitalState.fromBoolean(setStateAs))), (a, b) -> a);

        log.trace(request.toString());

        cacheBoardState(mapping, connectorService.send(mapping.connectorId(), request));
    }

}
