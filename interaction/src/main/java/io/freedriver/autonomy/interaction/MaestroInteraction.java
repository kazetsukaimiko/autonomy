package io.freedriver.autonomy.interaction;

import org.usb4java.javax.adapter.UsbPipeAdapter;

import javax.usb.*;
import javax.usb.event.UsbPipeDataEvent;
import javax.usb.event.UsbPipeErrorEvent;
import java.io.Closeable;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class MaestroInteraction implements AutoCloseable {
    private final Product product;
    private UsbDevice usbDevice;
    private UsbConfiguration usbConfiguration;
    private UsbInterface usbInterface;
    private UsbEndpoint usbEndpoint;
    private UsbPipe usbPipe;

    public MaestroInteraction(Product product) throws UsbException, IOException {
        this.product = product;
        assignUsbDevice();
    }

    private void assignUsbDevice() throws UsbException, IOException {
        if (usbInterface != null) {
            close();
        }
        usbDevice = findUsbDevices(UsbHostManager.getUsbServices().getRootUsbHub()).findFirst().orElseThrow(() ->
                new IllegalStateException("No Maestro Device for Product: " + product.toString()));
        usbConfiguration = usbDevice.getActiveUsbConfiguration();
        usbInterface = usbConfiguration.getUsbInterface((byte)0x02);

        printInterface(usbInterface);

        usbInterface.claim(usbInterface -> true);

        usbEndpoint = ((List<?>) usbInterface.getUsbEndpoints()).stream()
                .filter(UsbEndpoint.class::isInstance)
                .map(UsbEndpoint.class::cast)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No Endpoint for Maestro USB Device"));

        usbPipe = usbEndpoint.getUsbPipe();
        usbPipe.open();

    }

    public int getServoPosition(int channel) throws UsbException {
        // Send
        int responseLength = usbPipe.syncSubmit(getPositionCommandBytes(channel));

        // Read
        byte[] data = new byte[responseLength];
        usbPipe.syncSubmit(data);

        ByteBuffer wrapped = ByteBuffer.wrap(data);
        return Short.toUnsignedInt(wrapped.getShort());
    }







    public static byte[] getPositionCommandBytes(int servo) {
        byte command = (byte) 0x90;
        byte[] bytes = new byte[]{
                command,
                (byte) servo
        };
        return bytes;
    }

    public static byte[] servoCommandBytes(int channel, int target) {
        byte[] value = splitLowerAndHigher(target);
        return new byte[] {
                (byte) 0x84, // Compact
                (byte) channel,
                value[0],
                value[1]
        };
    }

    public static byte[] command(int position) {
        System.out.println("Command");
        byte[] value = splitLowerAndHigher(position);

        byte command = (byte) 0x84;
        int servo = 1;
        byte target1 = value[0];
        byte target2 = value[1];


        System.out.println("Target 1: " + (int) target1);
        System.out.println("Target 2: " + (int) target2);

        byte[] bytes = new byte[]{command,
                (byte) servo, target1, target2};

        return bytes;
    }


    public static byte[] mini(int position) {
        System.out.println("mini: "+position);
        //byte[] value = splitLowerAndHigher(position);

        byte command = (byte) 0xFF;
        int servo = 1;

        byte[] bytes = new byte[]{command,
                (byte) servo, (byte) position};

        return bytes;
    }

    public static byte[] goHome() {
        System.out.println("goHome");
        //byte[] value = splitLowerAndHigher(position);

        byte command = (byte) 0xA2;

        byte[] bytes = new byte[]{command};

        return bytes;
    }


    public static byte[] accelerationCommandBytes(int channel, int acceleration) {
        if (acceleration<0 || acceleration>255) {
            throw new IllegalArgumentException("Illegal Acceleration: must be 0-255");
        }
        byte[] accel = splitLowerAndHigher(acceleration);
        return new byte[] {
                (byte) 0x89,
                (byte) channel,
                accel[0],
                accel[1]
        };
    }

    public static byte[] splitLowerAndHigher(int toSplit) {
        return new byte[] {
                (byte) ((byte) toSplit & (byte) 0b01111111),
                (byte) ((toSplit >> 7) & 0b01111111)
        };
    }


    private void printEndpoint(UsbEndpoint usbEndpoint) {
        System.out.println(usbEndpoint);
    }

    private void printInterface(UsbInterface usbInterface) {
        try {
            System.out.println(usbInterface.getInterfaceString() + " : " + usbInterface.toString());
        } catch (UsbException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private Stream<UsbDevice> findUsbDevices(UsbHub usbHub) {
        return subDevices(usbHub)
                .peek(this::printDevice)
                .filter(product::matches);
    }

    private Stream<UsbDevice> subDevices(UsbDevice usbDevice) {
        if (usbDevice instanceof UsbHub) {
            return ((List<?>) ((UsbHub) usbDevice).getAttachedUsbDevices()).stream()
                    .filter(UsbDevice.class::isInstance)
                    .map(UsbDevice.class::cast)
                    .flatMap(this::subDevices);
        }
        return Stream.of(usbDevice);
    }

    private void printDevice(UsbDevice usbDevice) {
        UsbDeviceDescriptor usbDeviceDescriptor = usbDevice.getUsbDeviceDescriptor();


        System.out.println(
                Integer.toString(Short.toUnsignedInt(usbDeviceDescriptor.idVendor()), 16)
                + ":" +
                Integer.toString(Short.toUnsignedInt(usbDeviceDescriptor.idProduct()), 16));
    }

    private void doSomething() {
        //usbDevice.
    }

    @Override
    public void close() throws IOException {
        try {
            usbPipe.close();
            usbInterface.release();
        } catch (UsbException e) {
            throw new IOException("Cannot close USB Interface: ", e);
        }
    }
}
