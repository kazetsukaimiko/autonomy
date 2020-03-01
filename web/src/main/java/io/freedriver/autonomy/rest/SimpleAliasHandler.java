package io.freedriver.autonomy.rest;

import io.freedriver.autonomy.rest.provider.ObjectMapperContextResolver;
import io.freedriver.autonomy.service.ConnectorService;
import io.freedriver.jsonlink.config.Mapping;
import io.freedriver.jsonlink.config.Mappings;
import io.freedriver.jsonlink.jackson.schema.v1.Identifier;
import io.freedriver.util.file.FileProviders;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@RequestScoped
public class SimpleAliasHandler implements SimpleAliasApi {
    @Inject
    private ConnectorService connectorService;

    @Override
    public List<UUID> getBoards() {
        return connectorService.getConnectedBoards();
    }

    @Override
    public Map<Identifier, Boolean> getState(UUID boardId) throws IOException {
        return connectorService.readDigital(boardId, getMapping(boardId).getPinNames().keySet());
    }

    @Override
    public Map<Identifier, Boolean> setState(UUID boardId, Map<Identifier, Boolean> desiredState) {
        return connectorService.writeDigital(boardId, desiredState);
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
                FileProviders.MAPPINGS
                    .getProvider()
                    .get()
                    .toFile(),
                Mappings.class);
    }
}
