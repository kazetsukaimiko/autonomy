package io.freedriver.jsonlink;

import jssc.SerialPort;
import jssc.SerialPortException;

import java.io.IOException;

public class SerialConnector implements Connector {

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
                e.printStackTrace();
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

    private String readUntil(String delimiter) throws SerialPortException {
        long startTime = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder();
        while (true) {
            long readStart = System.currentTimeMillis();
            String character = serialPort.readString(1);
            long readTime = System.currentTimeMillis() - readStart;
            //System.out.println("Read took "+readTime+"ms");
            sb.append(character);
            if (sb.toString().endsWith(delimiter)) {
                long endTime = System.currentTimeMillis() - startTime;
                System.out.println("Took "+endTime+"ms");
                return sb.toString().substring(0, sb.length()-delimiter.length());
            }
        }
    }


}
