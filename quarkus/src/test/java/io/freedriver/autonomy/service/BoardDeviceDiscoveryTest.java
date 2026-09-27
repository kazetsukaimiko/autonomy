package io.freedriver.autonomy.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BoardDeviceDiscoveryTest {
    private static final String ARDUINO_BY_ID = "usb-Arduino__www.arduino.cc__0042_5573631333735171D1D1-if00";

    @TempDir
    Path tmp;

    @Test
    void vendorProductMatchIgnoresStaleByIdLink() throws IOException {
        Path sysClassTty = tmp.resolve("sys/class/tty");
        Path devRoot = tmp.resolve("dev");
        createUsbTty(sysClassTty, devRoot, "ttyACM1", "2341", "0042");
        Path byId = linkById(devRoot, "../../ttyACM0");
        assertFalse(Files.exists(devRoot.resolve("ttyACM0")));
        assertTrue(Files.isSymbolicLink(byId));

        List<Path> found = BoardDeviceDiscovery.discover(
                config("[{\"vendorId\":\"2341\",\"deviceId\":\"0042\"}]"), sysClassTty, devRoot);

        assertEquals(List.of(devRoot.resolve("ttyACM1")), found);
        assertFalse(found.stream().anyMatch(path -> path.toString().contains("by-id")));
    }

    @Test
    void vendorProductMatchUsesBoardWhenByIdPointsAtAnotherTtyAcm() throws IOException {
        Path sysClassTty = tmp.resolve("sys/class/tty");
        Path devRoot = tmp.resolve("dev");
        createUsbTty(sysClassTty, devRoot, "ttyACM0", "1a86", "7523");
        createUsbTty(sysClassTty, devRoot, "ttyACM1", "2341", "0042");
        Path byId = linkById(devRoot, "../../ttyACM0");
        assertEquals(devRoot.resolve("ttyACM0").toRealPath(), byId.toRealPath());

        List<Path> found = BoardDeviceDiscovery.discover(
                config("[{\"vendorId\":\"2341\",\"deviceId\":\"0042\"}]"), sysClassTty, devRoot);

        assertEquals(List.of(devRoot.resolve("ttyACM1")), found);
    }

    @Test
    void doesNotOpenTheSameBoardTwice() throws IOException {
        Path sysClassTty = tmp.resolve("sys/class/tty");
        Path devRoot = tmp.resolve("dev");
        createUsbTty(sysClassTty, devRoot, "ttyACM1", "2341", "0042");
        Path byId = linkById(devRoot, "../../ttyACM1");
        Path config = config(
                """
                [
                  { "path": "%s" },
                  { "vendorId": "0x2341", "deviceId": "42" },
                  { "vendorId": "2341", "deviceId": "0042" },
                  { "path": "%s" }
                ]
                """
                        .formatted(byId, devRoot.resolve("ttyACM1")));

        List<Path> found = BoardDeviceDiscovery.discover(config, sysClassTty, devRoot);

        assertEquals(1, found.size());
        assertEquals(devRoot.resolve("ttyACM1").toRealPath(), found.get(0).toRealPath());
        assertEquals("ttyACM1", found.get(0).getFileName().toString());
    }

    @Test
    void explicitIdsDoNotAlsoSelectArduinoDefaults() throws IOException {
        Path sysClassTty = tmp.resolve("sys/class/tty");
        Path devRoot = tmp.resolve("dev");
        createUsbTty(sysClassTty, devRoot, "ttyUSB0", "1a86", "7523");
        createUsbTty(sysClassTty, devRoot, "ttyACM1", "2341", "0042");

        List<Path> found = BoardDeviceDiscovery.discover(
                config("[{\"vendorId\":\"1a86\",\"deviceId\":\"7523\"}]"), sysClassTty, devRoot);

        assertEquals(List.of(devRoot.resolve("ttyUSB0")), found);
    }

    @Test
    void ignoreDevicesObjectUsesArduinoMegaDefaults() throws IOException {
        Path sysClassTty = tmp.resolve("sys/class/tty");
        Path devRoot = tmp.resolve("dev");
        createUsbTty(sysClassTty, devRoot, "ttyACM0", "1a86", "7523");
        createUsbTty(sysClassTty, devRoot, "ttyACM1", "2341", "0042");

        List<Path> found =
                BoardDeviceDiscovery.discover(config("{\"ignoreDevices\":[]}"), sysClassTty, devRoot);

        assertEquals(List.of(devRoot.resolve("ttyACM1")), found);
    }

    private Path config(String json) throws IOException {
        Path file = tmp.resolve("connectors.json");
        Files.writeString(file, json);
        return file;
    }

    private static Path linkById(Path devRoot, String relativeTarget) throws IOException {
        Path dir = devRoot.resolve("serial/by-id");
        Files.createDirectories(dir);
        Path link = dir.resolve(ARDUINO_BY_ID);
        Files.createSymbolicLink(link, Path.of(relativeTarget));
        return link;
    }

    private static void createUsbTty(Path sysClassTty, Path devRoot, String ttyName, String vendor, String product)
            throws IOException {
        Path usbDevice = sysClassTty.getParent().getParent().resolve("devices").resolve(ttyName + "-usb");
        Path iface = usbDevice.resolve(ttyName + "-iface");
        Files.createDirectories(iface);
        Files.writeString(usbDevice.resolve("idVendor"), vendor + "\n");
        Files.writeString(usbDevice.resolve("idProduct"), product + "\n");
        Path ttyDir = sysClassTty.resolve(ttyName);
        Files.createDirectories(ttyDir);
        Files.createSymbolicLink(ttyDir.resolve("device"), iface);
        Files.createDirectories(devRoot);
        Files.writeString(devRoot.resolve(ttyName), "");
    }
}
