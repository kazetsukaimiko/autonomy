package io.freedriver.jsonlink;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jssc.SerialPort;
import jssc.SerialPortList;

import java.util.Optional;
import java.util.stream.Stream;

public interface Connector {
    ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JsonLinkModule())
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

    /**
     * Send a request, receiving a response.
     */
    default Response send(Request request) throws ConnectorException {
        try {
            consumeJSON(MAPPER.writeValueAsString(request));
            return receiveResponse();
        } catch (JsonProcessingException e) {
            throw new ConnectorException("Couldn't marshall JSON", e);
        }
    }

    void consumeJSON(String json) throws ConnectorException;
    Response receiveResponse() throws ConnectorException;

    static Optional<Connector> getDefault() {
        return Stream.of(SerialPortList.getPortNames())
                .findFirst()
                .map(SerialPort::new)
                .map(SerialConnector::new);
    }
}
