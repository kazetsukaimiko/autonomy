package io.freedriver.jsonlink;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import io.freedriver.jsonlink.jackson.schema.v1.Request;
import io.freedriver.jsonlink.jackson.schema.v1.Response;
import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortTimeoutException;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoUnit.MINUTES;

public class SerialConnector implements Connector, AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(SerialConnector.class.getName());

    private final String device;
    private final SerialPort serialPort;

    private StringBuilder buffer = new StringBuilder();

    // Buffer to fetch responses
    private final Map<UUID, Response> responseMap = new ConcurrentHashMap<>();

    public SerialConnector(SerialPort serialPort) {
        this.device = serialPort.getPortName();
        this.serialPort = serialPort;

        if (!serialPort.isOpened()) {
            try {
                serialPort.openPort();
                serialPort.setParams(
                        SerialPort.BAUDRATE_115200,
                        SerialPort.DATABITS_8,
                        SerialPort.STOPBITS_1,
                        SerialPort.PARITY_NONE
                );
                //Thread.sleep(1000);
            } catch (SerialPortException e) {
                throw new ConnectorException(e);
            }
        }
    }

    private Map<UUID, Response> getResponseMap() {
        Set<UUID> expired = responseMap.values()
                .stream()
                .filter(response -> Instant.now().plus(Duration.of(1, MINUTES)).isAfter(response.getCreated()))
                .map(Response::getRequestId)
                .collect(Collectors.toSet());
        expired.stream()
            .peek(requestId -> LOGGER.warning("Request Id " + requestId + " was never consumed")) // TODO: Event
            .forEach(responseMap::remove);
        return responseMap;
    }

    @Override
    public Response send(Request request) throws ConnectorException {
        try {
            UUID requestId = UUID.randomUUID();
            request.setRequestId(requestId);
            String json = MAPPER.writeValueAsString(request);
            LOGGER.finest("Sending Request: ");
            LOGGER.finest(json);
            sendJSONRequest(json);
            return pollUntil(requestId)
                    .map(r -> r.logAnyErrors(err -> LOGGER.warning("Error from board: " + err)))
                    .orElseThrow(() -> new ConnectorException("Couldn't get response."));
        } catch (JsonProcessingException | SerialPortException e) {
            throw new ConnectorException("Couldn't marshall JSON", e);
        }
    }

    @Override
    public String device() {
        return device;
    }

    private synchronized void sendJSONRequest(String requestJSON) throws ConnectorException {
        try {
            LOGGER.log(Level.FINEST, requestJSON);
            serialPort.writeString(requestJSON);
        } catch (SerialPortException e) {
            throw new ConnectorException("Couldn't consume JSON", e);
        }
    }

    @Override
    public synchronized boolean isClosed() {
        return !serialPort.isOpened();
    }

    private synchronized Optional<Response> pollUntil(UUID requestId) throws SerialPortException {
        Instant start = Instant.now();
        while (true) {
            pollUntilFinish().ifPresent(responseJSON -> {
                try {
                    Response response = MAPPER.readValue(responseJSON, Response.class);
                    getResponseMap().put(response.getRequestId(), response);
                } catch (JsonProcessingException e) {
                    throw new ConnectorException("Couldn't consume JSON", e);
                }
            });
            if (getResponseMap().containsKey(requestId)) {
                return Optional.of(getResponseMap().remove(requestId));
            }
            if (Instant.now().isAfter(start.plus(Duration.of(1, MINUTES)))) {
                break;
            }
        }
        return Optional.empty();
    }

    private synchronized Optional<String> pollUntilFinish() throws SerialPortException {
        boolean invalidBuffer = buffer.length() > 0 && !buffer.toString().startsWith("{");
        while (true) {
            Optional<String> response = poll(10);
            if (response.isPresent()) {
                if (validate(response.get())) {
                    return response;
                } else {
                    LOGGER.warning("Invalid");
                }
            } else if (invalidBuffer && buffer.length() == 0) { // If we reset polling...
                return Optional.empty();
            }
        }
    }

    private static boolean validate(String input) {
        Map<Character, Integer> occurrenceMap = occurrenceMap(input);
        boolean valid =
                input.startsWith("{") && input.endsWith("}\n") && !input.contains(String.valueOf((char) 65533))
                    && occurrenceMap.getOrDefault("{", 0) == occurrenceMap.getOrDefault("}", 0)
                    && occurrenceMap.getOrDefault("[", 0) == occurrenceMap.getOrDefault("]", 0)
                ;
        if (!valid) {
            Connectors.getCallback()
                    .accept(input);
        }
        return valid;
    }

    private static Map<Character, Integer> occurrenceMap(String input) {
        Map<Character, Integer> om = new HashMap<>();
        if (input != null && !input.isEmpty()) {
            for (int i = 0; i < input.length(); i++) {
                Character key = input.charAt(i);
                if (!om.containsKey(key)) {
                    om.put(key, 1);
                } else {
                    om.put(key, om.get(key) + 1);
                }
            }
        }
        return om;
    }

    private synchronized Optional<String> poll(int timeout) throws SerialPortException {
        try {
            String character = serialPort.readString(1, timeout);
            buffer.append(character);
            if (buffer.toString().endsWith("\n")) {
                String result = buffer.toString();
                buffer = new StringBuilder();
                return Optional.of(result)
                        .filter(SerialConnector::validate);
            }
        } catch (SerialPortTimeoutException e) {
            LOGGER.log(Level.FINEST, "Poll timeout ", e);
        }
        return Optional.empty();
    }

    private synchronized String readUntil(String delimiter) throws SerialPortException {
        long startTime = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder();
        while (true) {
            String character = serialPort.readString(1);
            sb.append(character);
            if (sb.toString().endsWith(delimiter)) {
                long endTime = System.currentTimeMillis() - startTime;
                LOGGER.fine("Took "+endTime+"ms");
                return sb.toString().substring(0, sb.length()-delimiter.length());
            }
        }
    }


    @Override
    public synchronized void close() throws Exception {
        if (!isClosed()) {
            LOGGER.log(Level.WARNING, "Closing serialPort.");
            poll(100);
            serialPort.closePort();
        } else {
            LOGGER.log(Level.WARNING, "Tried to close serialPort, but was already closed.");
        }
    }
}
