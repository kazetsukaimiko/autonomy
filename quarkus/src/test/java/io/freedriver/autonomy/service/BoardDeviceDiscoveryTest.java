package io.freedriver.autonomy.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BoardDeviceDiscoveryTest {
    private static final String ARDUINO_BY_ID = "usb-Arduino__www.arduino.cc__0042_5573631333735171D1D1-if00";
    private static final String ARDUINO_IDS = "{\"vendorId\":\"2341\",\"deviceId\":\"0042\"}";

    @TempDir
    Path tmp;

    @Test
    void renumberedDeviceMovesFromTtyAcm0ToTtyAcm1() throws IOException {
        Path sysClassTty = tmp.resolve("sys/class/tty");
        Path devRoot = tmp.resolve("dev");
        Path config = config("[" + ARDUINO_IDS + "]");
        createUsbTty(sysClassTty, devRoot, "ttyACM0", "2341", "0042");

        List<Path> before = BoardDeviceDiscovery.discover(config, sysClassTty, devRoot);
        assertEquals(List.of(devRoot.resolve("ttyACM0")), before);

        removeTty(sysClassTty, devRoot, "ttyACM0");
        createUsbTty(sysClassTty, devRoot, "ttyACM1", "2341", "0042");

        List<Path> after = BoardDeviceDiscovery.discover(config, sysClassTty, devRoot);
        assertEquals(List.of(devRoot.resolve("ttyACM1")), after);
        assertTrue(after.stream().allMatch(BoardDeviceDiscovery::isTtyAcmNode));
    }

    @Test
    void missingByIdLinkStillSelectsMatchingTtyAcm() throws IOException {
        Path sysClassTty = tmp.resolve("sys/class/tty");
        Path devRoot = tmp.resolve("dev");
        createUsbTty(sysClassTty, devRoot, "ttyACM0", "2341", "0042");
        assertFalse(Files.exists(devRoot.resolve("serial/by-id")));

        List<Path> found = BoardDeviceDiscovery.discover(config("[" + ARDUINO_IDS + "]"), sysClassTty, devRoot);

        assertEquals(List.of(devRoot.resolve("ttyACM0")), found);
        assertFalse(found.stream().anyMatch(path -> path.toString().contains("by-id")));
    }

    @Test
    void staleByIdLinkPointingAtWrongTtyAcmIsIgnored() throws IOException {
        Path sysClassTty = tmp.resolve("sys/class/tty");
        Path devRoot = tmp.resolve("dev");
        createUsbTty(sysClassTty, devRoot, "ttyACM0", "1a86", "7523");
        createUsbTty(sysClassTty, devRoot, "ttyACM1", "2341", "0042");
        Path byId = linkById(devRoot, "../../ttyACM0");
        assertEquals(devRoot.resolve("ttyACM0").toRealPath(), byId.toRealPath());
        Path config = config(
                """
                [
                  { "path": "%s" },
                  { "vendorId": "2341", "deviceId": "0042" }
                ]
                """
                        .formatted(byId));

        List<Path> found = BoardDeviceDiscovery.discover(config, sysClassTty, devRoot);

        assertEquals(List.of(devRoot.resolve("ttyACM1")), found);
        assertFalse(found.contains(byId));
        assertFalse(found.contains(devRoot.resolve("ttyACM0")));
    }

    @Test
    void doesNotOpenTheSameBoardTwice() throws IOException {
        Path sysClassTty = tmp.resolve("sys/class/tty");
        Path devRoot = tmp.resolve("dev");
        Path iface = createUsbDevice(sysClassTty, "arduino", "2341", "0042");
        bindTty(sysClassTty, devRoot, "ttyACM0", iface);
        bindTty(sysClassTty, devRoot, "ttyACM1", iface);
        Path config = config(
                """
                [
                  { "vendorId": "2341", "deviceId": "0042" },
                  { "vendorId": "0x2341", "deviceId": "42" }
                ]
                """);

        List<Path> found = BoardDeviceDiscovery.discover(config, sysClassTty, devRoot);

        assertEquals(1, found.size());
        assertEquals(devRoot.resolve("ttyACM0"), found.get(0));
        assertEquals(found, BoardDeviceDiscovery.discover(config, sysClassTty, devRoot));
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
        bindTty(sysClassTty, devRoot, ttyName, createUsbDevice(sysClassTty, ttyName + "-usb", vendor, product));
    }

    private static Path createUsbDevice(Path sysClassTty, String name, String vendor, String product)
            throws IOException {
        Path usbDevice = sysClassTty.getParent().getParent().resolve("devices").resolve(name);
        Path iface = usbDevice.resolve(name + "-iface");
        Files.createDirectories(iface);
        Files.writeString(usbDevice.resolve("idVendor"), vendor + "\n");
        Files.writeString(usbDevice.resolve("idProduct"), product + "\n");
        return iface;
    }

    private static void bindTty(Path sysClassTty, Path devRoot, String ttyName, Path iface) throws IOException {
        Path ttyDir = sysClassTty.resolve(ttyName);
        Files.createDirectories(ttyDir);
        Path deviceLink = ttyDir.resolve("device");
        if (!Files.exists(deviceLink)) {
            Files.createSymbolicLink(deviceLink, iface);
        }
        Files.createDirectories(devRoot);
        Path node = devRoot.resolve(ttyName);
        if (!Files.exists(node)) {
            Files.writeString(node, "");
        }
    }

    private static void removeTty(Path sysClassTty, Path devRoot, String ttyName) throws IOException {
        Files.deleteIfExists(devRoot.resolve(ttyName));
        Path ttyDir = sysClassTty.resolve(ttyName);
        if (Files.exists(ttyDir.resolve("device"))) {
            Files.delete(ttyDir.resolve("device"));
        }
        if (Files.isDirectory(ttyDir)) {
            try (Stream<Path> children = Files.list(ttyDir)) {
                for (Path child : children.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(child);
                }
            }
            Files.deleteIfExists(ttyDir);
        }
    }
}
