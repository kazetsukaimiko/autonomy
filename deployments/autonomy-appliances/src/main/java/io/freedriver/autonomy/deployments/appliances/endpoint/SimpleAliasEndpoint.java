package io.freedriver.autonomy.deployments.appliances.endpoint;

import io.freedriver.autonomy.deployments.appliances.service.SimpleAliasService;
import io.freedriver.autonomy.jaxrs.endpoint.SimpleAliasApi;
import io.freedriver.autonomy.jaxrs.payload.AliasPayload;
import io.freedriver.jsonlink.config.v2.Appliance;
import io.freedriver.jsonlink.config.v2.Mapping;
import io.freedriver.jsonlink.jackson.schema.v1.Identifier;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@RequestScoped
@Path(SimpleAliasApi.ROOT)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SimpleAliasEndpoint implements SimpleAliasApi {
    private static final Logger LOGGER = Logger.getLogger(SimpleAliasEndpoint.class.getName());

    @Inject
    SimpleAliasService simpleAliasService;

    @Override
    public List<UUID> getBoards() {
        LOGGER.fine("Getting boards");
        return simpleAliasService.getConnectedBoards();
    }

    @Override
    public AliasPayload getState(UUID boardId) throws IOException {
        LOGGER.fine("Getting state of " + boardId);
        return makePayload(boardId);
    }

    @Override
    public AliasPayload setupBoard(UUID boardId) throws IOException {
        simpleAliasService.setupBoard(boardId);
        return getState(boardId);
    }

    @Override
    public AliasPayload setState(UUID boardId, Map<String, Boolean> desiredState) throws IOException {
        simpleAliasService.setState(boardId, desiredState);
        return getState(boardId);
    }

    @Override
    public AliasPayload setGroup(UUID boardId, String group, boolean desiredState) throws IOException {
        simpleAliasService.setState(boardId, simpleAliasService.getMapping(boardId)
                .getAppliances()
                .stream()
                .filter(appliance -> appliance.getGroups().contains(group))
                .collect(Collectors.toMap(
                        Appliance::getName,
                        app -> desiredState,
                        (a, b) -> b
                )));
        return getState(boardId);
    }

    @Override
    public AliasPayload setGroupViaGet(UUID boardId, String group, boolean desiredState) throws IOException {
        return setGroup(boardId, group, desiredState);
    }

    private AliasPayload makePayload(UUID boardId) throws IOException {
        Mapping mapping = simpleAliasService.getMapping(boardId);
        AliasPayload aliasPayload = new AliasPayload();

        Map<Identifier, Boolean> digitalState = simpleAliasService.currentState(boardId, mapping);
        aliasPayload.setApplianceStates(mapping.getAppliances()
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

        aliasPayload.setGroupStates(grouped.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> !e.getValue().contains(false)
                )));

        return aliasPayload;

    }

}
