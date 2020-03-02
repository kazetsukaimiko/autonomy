package io.freedriver.autonomy.rest;

import io.freedriver.autonomy.ee.Autonomy;
import io.freedriver.autonomy.rest.provider.ObjectMapperContextResolver;
import io.freedriver.autonomy.service.ConnectorService;
import io.freedriver.jsonlink.config.Mapping;
import io.freedriver.jsonlink.config.Mappings;
import io.freedriver.jsonlink.jackson.schema.v1.Identifier;
import io.freedriver.util.file.DirectoryProviders;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@RequestScoped
public class SimpleAliasHandler implements SimpleAliasApi {
    private static final Logger LOGGER = Logger.getLogger(SimpleAliasHandler.class.getName());

    @Inject
    private ConnectorService connectorService;

    @Override
    public List<UUID> getBoards() {
        LOGGER.info("Getting boards");
        return connectorService.getConnectedBoards();
    }

    @Override
    public Map<String, Boolean> getState(UUID boardId) throws IOException {
        LOGGER.info("Getting state of " + boardId);
        return aliases(boardId, connectorService.readDigital(boardId, getMapping(boardId).getPinNames().keySet()));
    }

    @Override
    public Map<String, Boolean> setState(UUID boardId, Map<String, Boolean> desiredState) throws IOException {
        if (!desiredState.isEmpty()) {
            LOGGER.info("Setting states:");
            desiredState.forEach((k, v) -> LOGGER.info(k+": " + (v ? "true":"false")));
            return aliases(boardId, connectorService.writeDigital(boardId, identifiers(boardId, desiredState)));
        }
        return getState(boardId);
    }

    private Map<Identifier, Boolean> identifiers(UUID boardId, Map<String, Boolean> desiredState) throws IOException {
        Map<String, Identifier> namedPins = getMapping(boardId).getNamedPins();
        return desiredState.keySet()
                .stream()
                .collect(Collectors.toMap(
                        namedPins::get,
                        desiredState::get,
                        (a, b) -> a
                ));
    }

    private Map<String, Boolean> aliases(UUID boardId, Map<Identifier, Boolean> actualState) throws IOException {
        Map<Identifier, String> pinNames = getMapping(boardId).getPinNames();
        return actualState.keySet()
                .stream()
                .collect(Collectors.toMap(
                        pinNames::get,
                        actualState::get,
                        (a, b) -> a
                ));
    }

    private Mapping getMapping(UUID boardId) throws IOException {
        return getMappings()
                .getMappings()
                .stream()
                .filter(mapping -> Objects.equals(boardId, mapping.getConnectorId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unable to find mapping for board " + boardId));
    }

    private Mappings getMappings() throws IOException {
        return ObjectMapperContextResolver.getMapper().readValue(
                DirectoryProviders.CONFIG
                        .getProvider()
                        .subdir(Autonomy.DEPLOYMENT)
                        .file("mappings.json")
                        .get()
                        .toFile(),
                Mappings.class);
    }
}
