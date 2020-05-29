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
import javax.ws.rs.WebApplicationException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
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

    public List<UUID> getConnectedBoards() {
        return getAllConnectors().stream()
                .map(Connector::getUUID)
                .collect(Collectors.toList());
    }

    /*
     * INTERNALS / HELPERS
     */

    private List<Connector> getAllConnectors() {
        Connectors.allConnectors()
                .filter(connector -> !ACTIVE_CONNECTORS.contains(connector))
                .peek(connector -> System.out.println("Adding connector device: " + connector.device()))
                .forEach(ACTIVE_CONNECTORS::add);
        return ACTIVE_CONNECTORS;
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
                .orElseThrow(() -> new WebApplicationException("Board not found", 404));
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
