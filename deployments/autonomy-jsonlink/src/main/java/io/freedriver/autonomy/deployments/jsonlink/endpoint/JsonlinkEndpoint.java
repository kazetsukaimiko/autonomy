package io.freedriver.autonomy.deployments.jsonlink.endpoint;

import io.freedriver.autonomy.deployments.common.api.JsonlinkApi;
import io.freedriver.autonomy.deployments.jsonlink.service.ConnectorService;
import io.freedriver.jsonlink.jackson.schema.v1.Request;
import io.freedriver.jsonlink.jackson.schema.v1.Response;

import javax.inject.Inject;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public class JsonlinkEndpoint implements JsonlinkApi {

    private static final Logger LOGGER = Logger.getLogger(JsonlinkEndpoint.class.getName());

    @Inject
    ConnectorService connectorService;

    @Override
    public Response send(UUID connectorId, Request request) {
        return connectorService.send(connectorId, request);
    }

    @Override
    public List<UUID> getBoards() {
        LOGGER.fine("Getting boards");
        return connectorService.getConnectedBoards();
    }
}
