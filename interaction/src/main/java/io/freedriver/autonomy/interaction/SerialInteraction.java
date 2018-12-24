package io.freedriver.autonomy.interaction;


import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortList;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Enumeration;

public class SerialInteraction {

    public SerialInteraction() {
    }


    /*
    static void listPorts() {
        java.util.Enumeration<CommPortIdentifier> portEnum = CommPortIdentifier.getPortIdentifiers();
    }

    static String getPortTypeName(int portType) {
        switch (portType) {
            case CommPortIdentifier.PORT_I2C:
                return "I2C";
            case CommPortIdentifier.PORT_PARALLEL:
                return "Parallel";
            case CommPortIdentifier.PORT_RAW:
                return "Raw";
            case CommPortIdentifier.PORT_RS485:
                return "RS485";
            case CommPortIdentifier.PORT_SERIAL:
                return "Serial";
            default:
                return "unknown type";
        }
    }*/

    public static void main(String[] args) throws SerialPortException, InterruptedException {
        Arrays.asList(SerialPortList.getPortNames())
                .forEach(System.out::println);

        jssc.SerialPort sp = new SerialPort("/dev/ttyACM2");
        if (sp.isOpened()) {
            sp.closePort();
        }

        try {
            sp.openPort();



            boolean success = sp.writeBytes(MaestroInteraction.command(6000));
            Thread.sleep(1500);
            success = sp.writeBytes(MaestroInteraction.command(2000));
            Thread.sleep(1500);
            success = sp.writeBytes(MaestroInteraction.command(12000));
            Thread.sleep(1500);

            success = sp.writeBytes(MaestroInteraction.goHome());

            if (success) {
                //int status = ByteBuffer.wrap(sp.readBytes(2)).getInt();
                //System.out.println(status);
            }
        } finally {
            System.out.println("Finally");
            if (sp.isOpened()) {
                sp.closePort();
            }
        }


    }
/*
    public static void ugh() {

        String portName = "COM5";

        try {

            listPorts();

            CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(portName);
            if (portIdentifier.isCurrentlyOwned()) {
                System.out.println("Error: Port is currently in use");
            } else {
                CommPort commPort = portIdentifier.open("Owner", 2000);
                if (commPort instanceof SerialPort) {

                    // we create a output stream for serial port:
                    SerialPort serialPort = (SerialPort) commPort;

                    serialPort.setSerialPortParams(9600, SerialPort.DATABITS_8,
                            SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

                    byte command = (byte) 0x84;
                    int servo = 0;
                    int target1 = 10;
                    int target2 = 10;

                    byte[] bytes = new byte[]{command,
                            (byte) servo, (byte) target1, (byte) target2};

                    byte command1 = (byte) 0x84;
                    int servo1 = 1;
                    int target11 = 10;
                    int target21 = 10;

                    byte[] bytes1 = new byte[]{command1,
                            (byte) servo1, (byte) target11, (byte) target21};

                    process(serialPort, bytes);
                    Thread.sleep(3000);
                    process(serialPort, bytes1);

                    commPort.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void process(SerialPort serialPort, byte[] bytes) throws IOException {
        OutputStream serialStream = serialPort.getOutputStream();
        serialStream.flush();
        serialStream.write(bytes);
        serialStream.close();
    }
    */
}