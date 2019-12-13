package io.freedriver.jsonlink;

import io.freedriver.jsonlink.jackson.schema.v1.Response;
import jssc.SerialPort;
import jssc.SerialPortException;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SerialConnector implements Connector, AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(SerialConnector.class.getName());

    private final  SerialPort serialPort;

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
            } catch (SerialPortException | InterruptedException e) {
                throw new ConnectorException(e);
            }
        }
    }

    @Override
    public void consumeJSON(String json) throws ConnectorException {
        try {
            serialPort.writeString(json);
        } catch (SerialPortException e) {
            throw new ConnectorException("Couldn't consume JSON", e);
        }
    }

    @Override
    public Response receiveResponse() throws ConnectorException {
        try {
            return MAPPER.readValue(readUntil("\n"), Response.class);
        } catch (IOException | SerialPortException e) {
            throw new ConnectorException("Couldn't receive response", e);
        }
    }

    @Override
    public boolean isClosed() {
        return !serialPort.isOpened();
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
            serialPort.closePort();
        } else {
            LOGGER.log(Level.WARNING, "Tried to close serialPort, but was already closed.");
        }
    }
}
