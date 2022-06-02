package io.freedriver.autonomy.deployments.jsonlink.service;

import io.freedriver.autonomy.deployments.jsonlink.config.JsonlinkConfig;
import io.freedriver.jsonlink.Connector;
import io.freedriver.jsonlink.Connectors;
import io.freedriver.jsonlink.jackson.schema.v1.AnalogRead;
import io.freedriver.jsonlink.jackson.schema.v1.DigitalWrite;
import io.freedriver.jsonlink.jackson.schema.v1.Identifier;
import io.freedriver.jsonlink.jackson.schema.v1.Request;
import io.freedriver.jsonlink.jackson.schema.v1.Response;
import io.quarkus.runtime.StartupEvent;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The service by which we interact with connectors.
 */
@ApplicationScoped
public abstract class ConnectorService {
    private static final Logger LOGGER = Logger.getLogger(ConnectorService.class.getName());
    private static final List<Connector> ACTIVE_CONNECTORS = new ArrayList<>();
    protected static ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    @Inject
    JsonlinkConfig config;

    public void logConnectorsAtBoot(@Observes StartupEvent ev) {
        getAllConnectors()
                .stream()
                .map(connector -> "Connector board id " + connector.getUUID() + " setup on " + connector.devicePath())
                .forEach(LOGGER::info);
    }

    public List<UUID> getConnectedBoards() {
        return getAllConnectors().stream()
                .map(Connector::getUUID)
                .collect(Collectors.toList());
    }

    /*
     * INTERNALS / HELPERS
     */
    protected List<Connector> getAllConnectors() {
        // Remove existing closed.
        List<Connector> closed = ACTIVE_CONNECTORS.stream()
                .filter(this::connectorIsClosed)
                .collect(Collectors.toList());
        ACTIVE_CONNECTORS.removeAll(closed);

        // Connect new
        List<CompletableFuture<Void>> threads = config.connectors().stream()
                .filter(device -> ACTIVE_CONNECTORS.stream()
                        .noneMatch(existing -> Objects.equals(device, Paths.get(existing.device()))))
                .map(device -> Connectors.findOrOpenAndConsume(device, executorService, ACTIVE_CONNECTORS::add))
                .collect(Collectors.toList());
        threads.forEach(this::waitForCompletion);

        return ACTIVE_CONNECTORS;
    }

    protected boolean connectorIsClosed(Connector connector) {
        return connector.
                isClosed();
    }

    protected void waitForCompletion(CompletableFuture<Void> voidCompletableFuture) {
        try {
            voidCompletableFuture.get();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to wait for completion of connector", e);
        }
    }

    protected Optional<Connector> getConnectorByBoardId(UUID boardId) {
        return getAllConnectors().stream()
                .filter(connector -> Objects.equals(boardId, connector.getUUID()))
                .findFirst();
    }

    public synchronized Response send(UUID uuid, Request request) {
        return getConnectorByBoardId(uuid)
                .map(connector -> connector.send(request))
                .orElseThrow(() -> new WebApplicationException("Board not found, present devices: " + ACTIVE_CONNECTORS.stream().map(Connector::device).collect(Collectors.joining(",")), 404));
    }

}
