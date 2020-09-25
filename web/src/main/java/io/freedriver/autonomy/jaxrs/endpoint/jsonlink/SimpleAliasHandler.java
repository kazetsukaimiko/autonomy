package io.freedriver.autonomy.jaxrs.endpoint.jsonlink;

import io.freedriver.autonomy.jaxrs.endpoint.SimpleAliasApi;
import io.freedriver.autonomy.jaxrs.view.AliasView;
import io.freedriver.autonomy.service.ConnectorService;
import io.freedriver.autonomy.service.SimpleAliasService;
import io.freedriver.jsonlink.config.v2.Appliance;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@RequestScoped
public class SimpleAliasHandler implements SimpleAliasApi {
    private static final Logger LOGGER = Logger.getLogger(SimpleAliasHandler.class.getName());

    @Inject
    private ConnectorService connectorService;

    @Inject
    private SimpleAliasService simpleAliasService;

    @Override
    public List<UUID> getBoards() {
        LOGGER.fine("Getting boards");
        return connectorService.getConnectedBoards();
    }

    @Override
    public AliasView getState(UUID boardId) throws IOException {
        LOGGER.fine("Getting state of " + boardId);
        return simpleAliasService.newView(boardId);
    }

    @Override
    public AliasView setupBoard(UUID boardId) throws IOException {
        simpleAliasService.setupBoard(boardId);
        return getState(boardId);
    }

    @Override
    public AliasView setState(UUID boardId, Map<String, Boolean> desiredState) throws IOException {
        simpleAliasService.setState(boardId, desiredState);
        return getState(boardId);
    }

    @Override
    public AliasView setGroup(UUID boardId, String group, boolean desiredState) throws IOException {
        connectorService.writeDigital(boardId, simpleAliasService.getMapping(boardId)
                .getAppliances()
                .stream()
                .filter(appliance -> appliance.getGroups().contains(group))
                .collect(Collectors.toMap(
                        Appliance::getIdentifier,
                        app -> desiredState,
                        (a, b) -> b
                )));
        return getState(boardId);
    }

}
