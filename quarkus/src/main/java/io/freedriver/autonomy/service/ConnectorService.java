package io.freedriver.autonomy.service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.freedriver.jsonlink.api.v1.AnalogRead;
import io.freedriver.jsonlink.api.v1.BoardRequest;
import io.freedriver.jsonlink.api.v1.BoardResponse;
import io.freedriver.jsonlink.api.v1.DigitalWrite;
import io.freedriver.jsonlink.api.v1.Identifier;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

/**
 * The service by which we interact with connectors.
 */
@ApplicationScoped
@Slf4j
public class ConnectorService extends ConnectorServiceCommon {
    //private static final List<Connector> ACTIVE_CONNECTORS = new ArrayList<>();
    private static final Path CONFIG_PATH = Paths.get(System.getProperty("user.home"), ".config/autonomy");
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);


    @Override
    protected void waitForCompletion(CompletableFuture<Void> voidCompletableFuture) {
        try {
            voidCompletableFuture.get();
        } catch (Exception e) {
            log.warn("Failed to wait for completion of connector", e);
        }
    }

    /*
    @Deprecated
    public synchronized Map<Identifier, Boolean> readDigital(UUID boardId, Collection<Identifier> pins) {
        return send(boardId, pins.stream()
                .reduce(BoardRequest.empty(), BoardRequest::digitalRead, (a, b) -> a))
                .digital();
    }

     */

    public synchronized BoardResponse readDigitalAndAnalog(UUID boardId, Collection<Identifier> pins, Stream<AnalogRead> analogReads) {
        return send(boardId, pins.stream()
                .reduce(BoardRequest.empty(), BoardRequest::digitalRead, (a, b) -> a)
                .analogRead(analogReads));
    }

    public synchronized Map<Identifier, Boolean> writeDigital(UUID boardId, Map<Identifier, Boolean> state) {
        BoardRequest request = state.entrySet().stream()
                .reduce(BoardRequest.empty(),
                        (req, e) -> req.digitalWrite(new DigitalWrite(e.getKey(), e.getValue())),
                        (a, b) -> a);
        return send(boardId, request)
                .digital();
    }
}
