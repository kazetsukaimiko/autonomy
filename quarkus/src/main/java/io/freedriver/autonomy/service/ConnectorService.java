package io.freedriver.autonomy.service;

import io.freedriver.jsonlink.Connector;
import io.freedriver.jsonlink.Connectors;
import io.freedriver.jsonlink.jackson.schema.v1.AnalogRead;
import io.freedriver.jsonlink.jackson.schema.v1.DigitalWrite;
import io.freedriver.jsonlink.jackson.schema.v1.Identifier;
import io.freedriver.jsonlink.jackson.schema.v1.Request;
import io.freedriver.jsonlink.jackson.schema.v1.Response;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.WebApplicationException;
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
public class ConnectorService {
    private static final Logger LOGGER = Logger.getLogger(ConnectorService.class.getName());

    private static final List<Connector> ACTIVE_CONNECTORS = new ArrayList<>();

    protected ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    /**
     * Detects if the Connector is closed.
     * @param connector
     * @return
     */
    protected boolean connectorIsClosed(Connector connector) {
        return connector.
                isClosed();
    }


    protected List<Connector> getAllConnectors() {
        // Remove existing closed.
        List<Connector> closed = ACTIVE_CONNECTORS.stream()
                .filter(this::connectorIsClosed)
                .collect(Collectors.toList());
        ACTIVE_CONNECTORS.removeAll(closed);

        // Connect new
        List<CompletableFuture<Void>> threads = Connectors.allDevices()
                .filter(device -> ACTIVE_CONNECTORS.stream()
                        .noneMatch(existing -> Objects.equals(existing.device(), device)))
                .map(device -> Connectors.findOrOpenAndConsume(device, executorService, ACTIVE_CONNECTORS::add))
                .collect(Collectors.toList());
        threads.forEach(this::waitForCompletion);

        return ACTIVE_CONNECTORS;
    }

    public List<UUID> getConnectedBoards() {
        return getAllConnectors().stream()
                .map(Connector::getUUID)
                .collect(Collectors.toList());
    }



    protected <T> void waitForCompletion(CompletableFuture<T> voidCompletableFuture) {
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

    // TODO: Refactor to take Mapping, Collection<Identifier> and infer DigitalRead/AnalogRead from the identifiers.
    public synchronized Response readPins(UUID boardId, Collection<Identifier> pins, Stream<AnalogRead> analogReads) {
        return send(boardId, pins.stream()
                .reduce(new Request(), Request::digitalRead, (a, b) -> a)
                .analogRead(analogReads));
    }

    public synchronized Map<Identifier, Boolean> writeDigital(UUID boardId, Map<Identifier, Boolean> state) {
        Request request = new Request();
        state.forEach((pin, pinState) -> request.digitalWrite(new DigitalWrite(pin, pinState)));
        return send(boardId, request)
                .getDigital();
    }
}
