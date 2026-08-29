package io.freedriver.autonomy.service;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.freedriver.jsonlink.Connector;
import io.freedriver.jsonlink.Connectors;
import io.freedriver.jsonlink.jackson.JsonLinkModule;
import io.freedriver.jsonlink.jackson.schema.v1.Request;
import io.freedriver.jsonlink.jackson.schema.v1.Response;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import lombok.extern.slf4j.Slf4j;

/**
 * The service by which we interact with connectors.
 */
@ApplicationScoped
@Slf4j
public class ConnectorServiceCommon {
    private static final List<Connector> ACTIVE_CONNECTORS = new CopyOnWriteArrayList<>();
    private static final Path CONFIG_PATH = Paths.get(System.getProperty("user.home"), ".config/autonomy");
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JsonLinkModule())
            .enable(SerializationFeature.INDENT_OUTPUT);


    protected ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public List<UUID> getConnectedBoards() {
        return getAllConnectors().stream()
                .map(this::uuidOrNull)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    /*
     * INTERNALS / HELPERS
     */
    protected synchronized List<Connector> getAllConnectors() {
        // Remove existing closed.
        List<Connector> closed = ACTIVE_CONNECTORS.stream()
                .filter(this::connectorIsClosed)
                .collect(Collectors.toList());
        ACTIVE_CONNECTORS.removeAll(closed);

        // Connect new. Match by canonical path so /dev/serial/by-id/... and /dev/ttyACM0
        // are not opened twice against the same Arduino.
        List<CompletableFuture<Void>> threads = Connectors.allDevices().stream()
                .filter(device -> ACTIVE_CONNECTORS.stream()
                        .noneMatch(existing -> sameDevice(existing, device)))
                .map(device -> Connectors.findOrOpenAndConsume(device, executorService, ACTIVE_CONNECTORS::add))
                .collect(Collectors.toList());
        threads.forEach(this::waitForCompletion);

        return ACTIVE_CONNECTORS;
    }

    private UUID uuidOrNull(Connector connector) {
        try {
            return connector.getUUID();
        } catch (Exception e) {
            log.warn("Couldn't read board UUID from {}", connector.device(), e);
            return null;
        }
    }

    private static boolean sameDevice(Connector existing, Path device) {
        return Objects.equals(canonical(existing.devicePath()), canonical(device));
    }

    private static Path canonical(Path path) {
        try {
            return path.toRealPath();
        } catch (IOException e) {
            return path.toAbsolutePath().normalize();
        }
    }

    protected boolean connectorIsClosed(Connector connector) {
        return connector.
                isClosed();
    }

    protected void waitForCompletion(CompletableFuture<Void> voidCompletableFuture) {
        try {
            voidCompletableFuture.get();
        } catch (Exception e) {
            log.warn("Failed to wait for completion of connector", e);
        }
    }

    protected Optional<Connector> getConnectorByBoardId(UUID boardId) {
        return getAllConnectors().stream()
                .filter(connector -> Objects.equals(boardId, uuidOrNull(connector)))
                .findFirst();
    }

/*
    public String describeBoards() {
        return getAllConnectors().stream()
                .map(Connector::getUUID)
                .sorted(Comparator.comparing(UUID::toString))
                .map(UUID::toString)
                .collect(Collectors.joining(","));
    }*/

    public synchronized Response send(UUID uuid, Request request) {
        return getConnectorByBoardId(uuid)
                .map(connector -> connector.send(request))
                .orElseThrow(() -> new WebApplicationException("Board not found, present devices: " + ACTIVE_CONNECTORS.stream().map(Connector::device).collect(Collectors.joining(",")), 404));
    }

    /*
    @Deprecated
    public synchronized Map<Identifier, Boolean> readDigital(UUID boardId, Collection<Identifier> pins) {
        return send(boardId, pins.stream()
                .reduce(Request.empty(), Request::digitalRead, (a, b) -> a))
                .digital();
    }

    public synchronized Response readDigitalAndAnalog(UUID boardId, Collection<Identifier> pins, Stream<AnalogRead> analogReads) {
        return send(boardId, pins.stream()
                .reduce(Request.empty(), Request::digitalRead, (a, b) -> a)
                .analogRead(analogReads));
    }

    public synchronized Map<Identifier, Boolean> writeDigital(UUID boardId, Map<Identifier, Boolean> state) {
        Request request = state.entrySet().stream()
                .reduce(Request.empty(),
                        (req, e) -> req.digitalWrite(new DigitalWrite(e.getKey(), e.getValue())),
                        (a, b) -> a);
        return send(boardId, request)
                .digital();
    }

     */
}
