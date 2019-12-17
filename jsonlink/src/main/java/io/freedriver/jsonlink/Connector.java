package io.freedriver.jsonlink;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.freedriver.jsonlink.jackson.schema.v1.Request;
import io.freedriver.jsonlink.jackson.schema.v1.Response;
import io.freedriver.jsonlink.jackson.JsonLinkModule;

import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

public interface Connector extends AutoCloseable {
    Logger LOGGER = Logger.getLogger(Connector.class.getName());
    ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JsonLinkModule())
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

    /**
     * Send a request, receiving a response.
     */
    default Response send(Request request) throws ConnectorException {
        try {
            String json = MAPPER.writeValueAsString(request);
            LOGGER.info("Sending Request: ");
            LOGGER.info(json);
            return sendJSONRequest(json)
                    .map(r -> r.logAnyErrors(err -> LOGGER.warning("Error from board: " + err)))
                    .get();
        } catch (JsonProcessingException e) {
            throw new ConnectorException("Couldn't marshall JSON", e);
        }
    }

    /**
     * Setup the board's UUID.
     */
    default UUID getUUID() throws ConnectorException {
        return Optional.of(new Request())
                .map(this::send)
                .map(Response::getUuid)
                .orElseGet(() -> send(new Request().newUuid()).getUuid());
    }

    default void processBoardOutput(String line) {
        Connectors.getCallback().accept(line);
    }

    String device();
    Optional<Response> sendJSONRequest(String json) throws ConnectorException;
    //Optional<Response> fetchResponse() throws ConnectorException;
    boolean isClosed();

}
