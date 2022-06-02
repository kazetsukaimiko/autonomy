package io.freedriver.autonomy.deployments.appliances.service;

import io.freedriver.autonomy.deployments.common.Autonomy;
import io.freedriver.autonomy.deployments.common.api.JsonlinkApi;
import io.freedriver.autonomy.deployments.common.provider.ObjectMapperContextResolver;
import io.freedriver.autonomy.deployments.joystick.cache.ConnectorCache;
import io.freedriver.base.util.file.DirectoryProviders;
import io.freedriver.jsonlink.config.v2.AnalogSensor;
import io.freedriver.jsonlink.config.v2.Appliance;
import io.freedriver.jsonlink.config.v2.Mapping;
import io.freedriver.jsonlink.config.v2.Mappings;
import io.freedriver.jsonlink.jackson.schema.v1.AnalogRead;
import io.freedriver.jsonlink.jackson.schema.v1.DigitalWrite;
import io.freedriver.jsonlink.jackson.schema.v1.Identifier;
import io.freedriver.jsonlink.jackson.schema.v1.Mode;
import io.freedriver.jsonlink.jackson.schema.v1.ModeSet;
import io.freedriver.jsonlink.jackson.schema.v1.Request;
import io.freedriver.jsonlink.jackson.schema.v1.Response;
import io.freedriver.jsonlink.pin.PinCoordinate;
import org.infinispan.Cache;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class SimpleAliasService  {
    private static final Logger LOGGER = Logger.getLogger(SimpleAliasService.class.getName());

    @Inject
    JsonlinkApi jsonlinkApi;

    @Inject
    @ConnectorCache
    Cache<PinCoordinate, Boolean> digitalPinCache;

    public void waitFor(Duration duration) throws InterruptedException {
        Thread.sleep(duration.toMillis());
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

    public Map<Identifier, Boolean> currentState(UUID boardId, Mapping mapping)  {
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
        return cacheBoardState(mapping, readDigitalAndAnalog(
                        mapping.getAppliances().stream().map(Appliance::getIdentifier).collect(Collectors.toSet()),
                        mapping.getAnalogSensors().stream().map(AnalogSensor::asAnalogRead))
        ).getDigital();
    }


    public Request readDigitalAndAnalog(Collection<Identifier> pins, Stream<AnalogRead> analogReads) {
        return pins.stream()
                .reduce(new Request(), Request::digitalRead, (a, b) -> a)
                .analogRead(analogReads);
    }

    public Map<Identifier, Boolean> cacheBoardDigitalState(UUID boardId, Map<Identifier, Boolean> digitalState) {
        digitalState.forEach((k, v) -> digitalPinCache.put(new PinCoordinate(boardId, k), v));
        return digitalState;
    }

    public Response cacheBoardState(Mapping mapping, Request request) {
        Response currentState = jsonlinkApi.send(mapping.getConnectorId(), request);
        // Cache Digital Pins
        currentState.getDigital().forEach((k, v) ->
                digitalPinCache.put(new PinCoordinate(mapping.getConnectorId(), k), v));

        return currentState;
    }

    public Response setState(UUID boardId, Map<String, Boolean> desiredState) throws IOException {
        LOGGER.info("Setting states on board " + boardId + ":");
        desiredState.forEach((k, v) -> LOGGER.info(k+": " + (v ? "true":"false")));
        return setStateByIdentifiers(boardId, identifiers(boardId, desiredState));
    }

    public Response setStateByIdentifiers(UUID boardId, Map<Identifier, Boolean> desiredState) throws IOException {
        Request r = new Request();
        desiredState
                .entrySet()
                .stream()
                .map(SimpleAliasService::digitalWriteEntry)
                .forEach(r::digitalWrite);
        return cacheBoardState(getMapping(boardId), r);
    }

    private static DigitalWrite digitalWriteEntry(Map.Entry<Identifier, Boolean> entry) {
        return new DigitalWrite(entry.getKey(), entry.getValue());
    }

    public Response setupBoard(UUID boardId) throws IOException {
        Request request = new Request();
        Mapping mapping = getMapping(boardId);
        mapping
                .getAppliances()
                .stream()
                .map(Appliance::getIdentifier)
                .map(id -> new ModeSet(id, Mode.OUTPUT))
                .forEach(request::modeSet);
        return cacheBoardState(mapping, request);
    }

    public List<UUID> getConnectedBoards() {
        return jsonlinkApi.getBoards();
    }
}
