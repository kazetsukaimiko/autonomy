package io.freedriver.autonomy.rest;

import io.freedriver.autonomy.ee.Autonomy;
import io.freedriver.autonomy.rest.provider.ObjectMapperContextResolver;
import io.freedriver.autonomy.rest.view.AliasView;
import io.freedriver.autonomy.service.ConnectorService;
import io.freedriver.jsonlink.config.v2.Appliance;
import io.freedriver.jsonlink.config.v2.Mapping;
import io.freedriver.jsonlink.config.v2.Mappings;
import io.freedriver.jsonlink.jackson.schema.v1.Identifier;
import io.freedriver.jsonlink.jackson.schema.v1.Mode;
import io.freedriver.jsonlink.jackson.schema.v1.ModeSet;
import io.freedriver.jsonlink.jackson.schema.v1.Request;
import io.freedriver.util.file.DirectoryProviders;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
    public AliasView getState(UUID boardId) throws IOException {
        LOGGER.info("Getting state of " + boardId);
        return newView(boardId);
    }

    @Override
    public AliasView setupBoard(UUID boardId) throws IOException {
        Request request = new Request();
        getMapping(boardId)
                .getAppliances()
                .stream()
                .map(Appliance::getIdentifier)
                .map(id -> new ModeSet(id, Mode.OUTPUT))
                .forEach(request::modeSet);
        connectorService.send(boardId, request);
        return getState(boardId);
    }

    @Override
    public AliasView setState(UUID boardId, Map<String, Boolean> desiredState) throws IOException {
        if (!desiredState.isEmpty()) {
            LOGGER.info("Setting states:");
            desiredState.forEach((k, v) -> LOGGER.info(k+": " + (v ? "true":"false")));
            connectorService.writeDigital(boardId, identifiers(boardId, desiredState));
        }
        return getState(boardId);
    }

    @Override
    public AliasView setGroup(UUID boardId, String group, boolean desiredState) throws IOException {
        connectorService.writeDigital(boardId, getMapping(boardId)
                .getAppliances()
                .stream()
                .filter(appliance -> appliance.getGroups().contains(group))
                .collect(Collectors.toMap(
                        Appliance::getIdentifier,
                        app -> desiredState,
                        (a, b) -> b
                )));
        return newView(boardId);
    }

    /**
     * Conversion from aliases to their mapped pin numbers for controller i/o.
     */
    private Map<Identifier, Boolean> identifiers(UUID boardId, Map<String, Boolean> desiredState) throws IOException {
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

    private Mapping getMapping(UUID boardId) throws IOException {
        return getMappings()
                .getMappings()
                .stream()
                .filter(mapping -> Objects.equals(boardId, mapping.getConnectorId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unable to find mapping for board " + boardId));
    }

    private Mappings getMappings() throws IOException {
        Path v2File = Paths.get("mappings_v2.json");
        if (!Files.exists(v2File)) {
            Mappings mappings = ObjectMapperContextResolver.getMapper().readValue(
                    DirectoryProviders.CONFIG
                            .getProvider()
                            .subdir(Autonomy.DEPLOYMENT)
                            .file("mappings.json")
                            .get()
                            .toFile(),
                    io.freedriver.jsonlink.config.Mappings.class)
                    .migrate();
            ObjectMapperContextResolver.getMapper().writeValue(v2File.toFile(), mappings);
            return mappings;
        }
        return ObjectMapperContextResolver.getMapper().readValue(
                DirectoryProviders.CONFIG
                        .getProvider()
                        .subdir(Autonomy.DEPLOYMENT)
                        .file("mappings_v2.json")
                        .get()
                        .toFile(),
                Mappings.class);
    }

    private Map<Identifier, Boolean> currentState(UUID boardId, Mapping mapping) {
        return connectorService.readDigital(boardId, mapping.getAppliances()
                .stream().map(Appliance::getIdentifier).collect(Collectors.toSet()));
    }

    private AliasView newView(UUID boardId) throws IOException {
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
}
