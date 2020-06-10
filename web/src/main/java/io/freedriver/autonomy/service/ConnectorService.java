package io.freedriver.autonomy.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.freedriver.jsonlink.Connector;
import io.freedriver.jsonlink.Connectors;
import io.freedriver.jsonlink.jackson.JsonLinkModule;
import io.freedriver.jsonlink.jackson.schema.v1.DigitalWrite;
import io.freedriver.jsonlink.jackson.schema.v1.Identifier;
import io.freedriver.jsonlink.jackson.schema.v1.Request;
import io.freedriver.jsonlink.jackson.schema.v1.Response;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * The service by which
 */
@ApplicationScoped
public class ConnectorService {
    private static final Logger LOGGER = Logger.getLogger(ConnectorService.class.getName());
    private static final List<Connector> ACTIVE_CONNECTORS = new ArrayList<>();
    private static final Path CONFIG_PATH = Paths.get(System.getProperty("user.home"), ".config/autonomy");
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JsonLinkModule())
            .enable(SerializationFeature.INDENT_OUTPUT);


    @Inject
    private ExecutorService executorService;

    public List<UUID> getConnectedBoards() {
        return getAllConnectors().stream()
                .map(Connector::getUUID)
                .collect(Collectors.toList());
    }

    /*
     * INTERNALS / HELPERS
     */

    private List<Connector> getAllConnectors() {
        // Remove existing closed.
        List<Connector> closed = ACTIVE_CONNECTORS.stream()
                .filter(Connector::isClosed)
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

    private void waitForCompletion(CompletableFuture<Void> voidCompletableFuture) {
        try {
            voidCompletableFuture.get();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to wait for completion of connector", e);
        }
    }


    private Optional<Connector> getConnectorByBoardId(UUID boardId) {
        return getAllConnectors().stream()
                .filter(connector -> Objects.equals(boardId, connector.getUUID()))
                .findFirst();
    }

    private static Path inConfigDirectory(String name) {
        return Paths.get(CONFIG_PATH.toAbsolutePath().toString(), name);
    }

    public String describeBoards() {
        return getAllConnectors().stream()
                .map(Connector::getUUID)
                .sorted(Comparator.comparing(UUID::toString))
                .map(UUID::toString)
                .collect(Collectors.joining(","));
    }

    public synchronized Response send(UUID uuid, Request request) {
        return getConnectorByBoardId(uuid)
                .map(connector -> connector.send(request))
                .orElseThrow(() -> new WebApplicationException("Board not found, present devices: " + ACTIVE_CONNECTORS.stream().map(Connector::device).collect(Collectors.joining(",")), 404));
    }

    public synchronized Map<Identifier, Boolean> readDigital(UUID boardId, Collection<Identifier> pins) {
        return send(boardId, pins.stream()
                .reduce(new Request(), Request::digitalRead, (a, b) -> a))
                .getDigital();
    }

    public synchronized Map<Identifier, Boolean> writeDigital(UUID boardId, Map<Identifier, Boolean> state) {
        Request request = new Request();
        state.forEach((pin, pinState) -> request.digitalWrite(new DigitalWrite(pin, pinState)));
        return send(boardId, request)
                .getDigital();
    }
}
