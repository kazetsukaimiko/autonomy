package io.freedriver.jsonlink;

import io.freedriver.jsonlink.jackson.schema.v1.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.usb.UsbDevice;
import javax.usb.UsbException;
import javax.usb.UsbHostManager;
import javax.usb.UsbHub;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class ConnectorTest {
    private static final Logger LOGGER = Logger.getLogger(ConnectorTest.class.getName());
    private static final ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()*4);
    static Identifier LED_PIN = Identifier.of(40); // Hallway

    List<UUID> allUUIDs;

    @BeforeEach
    public void init() {
        allUUIDs = Connectors.allDevices()
                .flatMap(ConnectorTest::openDevice)
                .map(Connector::getUUID)
                .distinct()
                .peek(uuid -> LOGGER.info(String.valueOf(uuid)))
                .collect(Collectors.toList());
    }


    @Test
    public void identifyConnectorsByUSB() throws UsbException {
        getAllUsbDevices(UsbHostManager.getUsbServices()
                .getRootUsbHub())
                .forEach(d -> System.out.println(d));
    }

    @SuppressWarnings("unchecked")
    public Stream<UsbDevice> getAllUsbDevices(UsbHub hub) {
        List<UsbDevice> devices = (List<UsbDevice>) hub.getAttachedUsbDevices();
        return devices.isEmpty()
                ? Stream.empty()
                : Stream.concat(
                    devices
                            .stream()
                            .filter(device -> !device.isUsbHub()),
                    devices
                            .stream()
                            .filter(UsbDevice::isUsbHub)
                            .map(UsbHub.class::cast)
                            .flatMap(this::getAllUsbDevices)
                );
    }

    @Test
    public void testGetBoardInfo() {
        Request boardInfo = new Request();
        boardInfo.setBoardInfo(true);

        Connectors.allDevices()
                .flatMap(ConnectorTest::openDevice)
                .findFirst()
                .map(connector -> connector.send(boardInfo))
                .ifPresent(System.out::println);
    }

    @Test
    public void testReadAnalogs() {
        Request readAllAnalogs = new Request();

        IntStream.range(87, 89)
                .mapToObj(Identifier::new)
                .map(identifier -> new AnalogRead(identifier, 5.0f, 250f))
                .peek(System.out::println)
                .forEach(readAllAnalogs::analogRead);

        Connectors.allDevices()
                .flatMap(ConnectorTest::openDevice)
                .findFirst()
                .ifPresent(connector -> {

                    for(int i=0;i<10;i++) {
                        Response r = connector.send(readAllAnalogs);
                        r.getAnalog()
                                .forEach(System.out::println);
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    @Test
    public void testGetConnectors() {
        Connectors.allDevices()
                .flatMap(ConnectorTest::openDevice)
                .forEach(connector -> System.out.println(connector.getUUID()));
    }

    @Test
    public void eachConnectorBlinks() {
        allUUIDs.forEach(uuid -> Connectors.getConnector(uuid)
                .ifPresent(connector -> {
                    Response modeResponse = connector.send(new Request().modeSet(new ModeSet(LED_PIN, Mode.OUTPUT)));
                    assertFalse(modeResponse.getError().stream()
                        .anyMatch(errorString -> errorString.toLowerCase()
                                .contains("invalid digital pin")));
                    IntStream.range(0, 10)
                            .forEach(i -> setStatus(connector, DigitalState.fromBoolean(i % 2 == 0)));
                }));
    }


    public static Stream<Connector> openDevice(String device) {
        try {
            return Connectors.findOrOpen(device, pool)
                    .get()
                    .stream();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Response setStatus(Connector connector, DigitalState pinState) {
        LOGGER.info("Setting status : " + pinState);
        Response r = connector.send(new Request()
                .digitalWrite(new DigitalWrite(LED_PIN, pinState)));
        delay(Duration.of(250, MILLIS));
        return r;
    }

    private static void delay(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
