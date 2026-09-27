package io.freedriver.autonomy.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

/**
 * Opens board serial ports from {@code ~/.config/jsonlink/connectors.json}.
 *
 * <p>Each {@code /dev/ttyACM*} node is checked against sysfs {@code idVendor} and
 * {@code idProduct}. A node is opened when those ids are listed in connectors.json,
 * under whatever ttyACM number that USB device has right now. One USB device is
 * returned once.
 */
@Slf4j
public final class BoardDeviceDiscovery {
    private static final Path CONFIG_FILE =
            Path.of(System.getProperty("user.home"), ".config", "jsonlink", "connectors.json");
    private static final Path SYS_CLASS_TTY = Path.of("/sys/class/tty");
    private static final Path DEV_ROOT = Path.of("/dev");
    private static final Pattern TTY_ACM = Pattern.compile("ttyACM\\d+");
    private static final Pattern HEX = Pattern.compile("[0-9a-f]{1,4}");
    private static final int MAX_SYSFS_WALK = 16;
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Object LOG_LOCK = new Object();
    private static List<Path> lastResolved;

    private BoardDeviceDiscovery() {}

    public static List<Path> discover() {
        return discover(CONFIG_FILE, SYS_CLASS_TTY, DEV_ROOT);
    }

    static List<Path> discover(Path connectorsJson, Path sysClassTty, Path devRoot) {
        Set<UsbId> wanted = load(connectorsJson);
        if (wanted.isEmpty()) {
            return publish(List.of());
        }
        LinkedHashMap<Path, Path> unique = new LinkedHashMap<>();
        for (Path node : ttyAcmNodes(devRoot)) {
            usbDeviceOf(sysClassTty.resolve(node.getFileName()))
                    .filter(device -> wanted.contains(device.id()))
                    .ifPresent(device -> unique.putIfAbsent(
                            canonical(device.deviceDir()), node.toAbsolutePath().normalize()));
        }
        return publish(List.copyOf(unique.values()));
    }

    static boolean isTtyAcmNode(Path device) {
        Path name = device == null ? null : device.getFileName();
        return name != null && TTY_ACM.matcher(name.toString()).matches();
    }

    private static List<Path> ttyAcmNodes(Path devRoot) {
        if (!Files.isDirectory(devRoot)) {
            log.warn("ttyACM discovery skipped: {} is not a directory", devRoot);
            return List.of();
        }
        try (Stream<Path> entries = Files.list(devRoot)) {
            return entries.filter(BoardDeviceDiscovery::isTtyAcmNode)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
        } catch (IOException e) {
            log.warn("Failed to enumerate ttyACM devices in {}", devRoot, e);
            return List.of();
        }
    }

    private static List<Path> publish(List<Path> resolved) {
        synchronized (LOG_LOCK) {
            if (Objects.equals(resolved, lastResolved)) {
                return resolved;
            }
            lastResolved = resolved;
        }
        if (resolved.isEmpty()) {
            log.warn("connectors.json USB ids matched no ttyACM device");
        } else {
            log.info("Resolved connectors.json USB ids to {}", resolved);
        }
        return resolved;
    }

    private static Set<UsbId> load(Path connectorsJson) {
        if (connectorsJson == null || !Files.isRegularFile(connectorsJson)) {
            log.warn("connectors.json missing at {}; using Arduino Mega USB id defaults", connectorsJson);
            return arduinoMegaDefaults();
        }
        try {
            JsonNode node = MAPPER.readTree(connectorsJson.toFile());
            if (node.isArray()) {
                return parseArray(node);
            }
            if (node.isObject() && node.path("devices").isArray()) {
                return parseArray(node.get("devices"));
            }
            log.warn(
                    "connectors.json at {} is not a device list; using Arduino Mega USB id defaults",
                    connectorsJson);
            return arduinoMegaDefaults();
        } catch (IOException e) {
            log.warn("Couldn't read connectors.json at {}; using Arduino Mega USB id defaults", connectorsJson, e);
            return arduinoMegaDefaults();
        }
    }

    private static Set<UsbId> parseArray(JsonNode array) {
        LinkedHashSet<UsbId> ids = new LinkedHashSet<>();
        for (JsonNode entry : array) {
            if (!entry.isObject()) {
                log.warn("Skipping connectors.json entry {}", entry);
                continue;
            }
            String vendor = text(entry, "vendorId", "vendor", "idVendor");
            String product = text(entry, "deviceId", "productId", "product", "idProduct");
            if (vendor == null || product == null) {
                log.warn("Skipping connectors.json entry without USB ids {}", entry);
                continue;
            }
            UsbId.tryParse(vendor, product)
                    .ifPresentOrElse(
                            ids::add, () -> log.warn("Skipping connectors.json USB id {}:{}", vendor, product));
        }
        return Set.copyOf(ids);
    }

    private static String text(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value == null || value.isNull()) {
                continue;
            }
            String text = value.asText();
            if (text != null && !text.isBlank()) {
                return text.trim();
            }
        }
        return null;
    }

    private static Set<UsbId> arduinoMegaDefaults() {
        return Set.of(new UsbId("2341", "0042"), new UsbId("2a03", "0042"));
    }

    private static Optional<UsbDevice> usbDeviceOf(Path sysTtyDir) {
        Path deviceLink = sysTtyDir.resolve("device");
        if (!Files.exists(deviceLink)) {
            return Optional.empty();
        }
        try {
            Path current = deviceLink.toRealPath();
            for (int i = 0; i < MAX_SYSFS_WALK && current != null; i++) {
                Path vendor = current.resolve("idVendor");
                Path product = current.resolve("idProduct");
                if (Files.isRegularFile(vendor) && Files.isRegularFile(product)) {
                    Path deviceDir = current;
                    return UsbId.tryParse(Files.readString(vendor).trim(), Files.readString(product).trim())
                            .map(id -> new UsbDevice(id, deviceDir));
                }
                current = current.getParent();
            }
        } catch (IOException e) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    private static Path canonical(Path path) {
        try {
            return path.toRealPath();
        } catch (IOException e) {
            return path.toAbsolutePath().normalize();
        }
    }

    private record UsbDevice(UsbId id, Path deviceDir) {}

    private record UsbId(String vendor, String product) {
        UsbId {
            vendor = normalize(vendor);
            product = normalize(product);
        }

        static Optional<UsbId> tryParse(String vendor, String product) {
            try {
                return Optional.of(new UsbId(vendor, product));
            } catch (IllegalArgumentException e) {
                return Optional.empty();
            }
        }

        @Override
        public String toString() {
            return vendor + ":" + product;
        }

        private static String normalize(String value) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("USB id component required");
            }
            String hex = value.trim().toLowerCase(Locale.ROOT);
            if (hex.startsWith("0x")) {
                hex = hex.substring(2);
            }
            if (!HEX.matcher(hex).matches()) {
                throw new IllegalArgumentException("Invalid USB id component: " + value);
            }
            return "0".repeat(4 - hex.length()) + hex;
        }
    }
}
