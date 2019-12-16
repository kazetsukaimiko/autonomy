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
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class SerialConnector implements Connector, AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(SerialConnector.class.getName());

    private final  SerialPort serialPort;

    private StringBuilder buffer = new StringBuilder();

    public SerialConnector(SerialPort serialPort) {
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
                Thread.sleep(1000);

                Optional<String> packet = poll(100);
                if (packet.isEmpty() && buffer.length() > 0) {
                    pollUntilFinish();
                }
                send(new Request());
            } catch (SerialPortException | InterruptedException e) {
                throw new ConnectorException(e);
            }
        }
    }

    @Override
    public void consumeJSON(String json) throws ConnectorException {
        try {
            LOGGER.info("Payload: ");
            LOGGER.info(json);
            serialPort.writeString(json);
        } catch (SerialPortException e) {
            throw new ConnectorException("Couldn't consume JSON", e);
        }
    }

    @Override
    public Response receiveResponse() throws ConnectorException {
        try {
            String jsonResponse = readUntil("\n");
            LOGGER.info(jsonResponse);
            return MAPPER.readValue(jsonResponse, Response.class);
        } catch (IOException | SerialPortException e) {
            throw new ConnectorException("Couldn't receive response", e);
        }
    }

    @Override
    public boolean isClosed() {
        return !serialPort.isOpened();
    }

    @Override
    public Optional<Response> fetchResponse() throws ConnectorException {
        try {
            Optional<String> json = pollUntilFinish();
            if (json.isPresent()) {
                return Optional.of(MAPPER.readValue(json.get(), Response.class));
            } else {
                return Optional.empty();
            }
        } catch (IOException | SerialPortException e) {
            throw new ConnectorException("Couldn't poll",e);
        }
    }

    private Optional<String> pollUntilFinish() throws SerialPortException {
        boolean invalidBuffer = buffer.length() > 0 && !buffer.toString().startsWith("{");
        while (true) {
            Optional<String> response = poll(10);
            if (response.isPresent()) {
                return response;
            } else if (invalidBuffer && buffer.length() == 0) { // If we reset polling...
                return Optional.empty();
            }
        }
    }

    private static boolean validate(String input) {
        Map<Character, Integer> occurrenceMap = occurrenceMap(input);
        return
                input.startsWith("{") && input.endsWith("}\n") && !input.contains(String.valueOf((char) 65533))
                    && occurrenceMap.getOrDefault("{", 0) == occurrenceMap.getOrDefault("}", 0)
                    && occurrenceMap.getOrDefault("[", 0) == occurrenceMap.getOrDefault("]", 0)
                ;
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
            if (buffer.toString().endsWith("}\n")) {
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

    private String readUntil(String delimiter) throws SerialPortException {
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
    public void close() throws Exception {
        if (!isClosed()) {
            LOGGER.log(Level.WARNING, "Closing serialPort.");
            poll(100);
            serialPort.closePort();
        } else {
            LOGGER.log(Level.WARNING, "Tried to close serialPort, but was already closed.");
        }
    }
}
