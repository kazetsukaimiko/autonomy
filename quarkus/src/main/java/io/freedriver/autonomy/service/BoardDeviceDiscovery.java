package io.freedriver.autonomy.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

/**
 * Finds board device nodes from {@code ~/.config/jsonlink/connectors.json}.
 *
 * <p>Vendor and product ids are matched against sysfs {@code idVendor}/{@code idProduct}
 * and resolved to the {@code /dev/ttyACM*} or {@code /dev/ttyUSB*} node that currently
 * has those ids. A {@code /dev/serial/by-id} symlink is not used as the open path, so a
 * re-enumeration or a stale link still opens the board that owns the configured ids.
 * Paths that canonicalize to the same node are returned once.
 */
@Slf4j
public final class BoardDeviceDiscovery {
    private static final Path CONFIG_FILE =
            Path.of(System.getProperty("user.home"), ".config", "jsonlink", "connectors.json");
    private static final Path SYS_CLASS_TTY = Path.of("/sys/class/tty");
    private static final Path DEV_ROOT = Path.of("/dev");
    private static final Pattern TTY_NAME = Pattern.compile("tty(ACM|USB)\\d+");
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
        List<DeviceSpec> specs = load(connectorsJson);
        if (specs.stream().anyMatch(spec -> spec instanceof VendorProduct)
                && !Files.isDirectory(sysClassTty)) {
            log.warn("USB sysfs discovery skipped: {} is not a directory", sysClassTty);
        }
        LinkedHashMap<Path, Path> unique = new LinkedHashMap<>();
        for (DeviceSpec spec : specs) {
            for (Path path : spec.resolve(sysClassTty, devRoot)) {
                remember(unique, path);
            }
        }
        List<Path> resolved = List.copyOf(unique.values());
        logResolved(resolved);
        return resolved;
    }

    private static void remember(LinkedHashMap<Path, Path> unique, Path path) {
        Path key = canonical(path);
        Path absolute = path.toAbsolutePath().normalize();
        Path previous = unique.get(key);
        if (previous == null || (!isSerialNode(previous) && isSerialNode(absolute))) {
            unique.put(key, absolute);
        }
    }

    private static boolean isSerialNode(Path path) {
        Path name = path.getFileName();
        return name != null && TTY_NAME.matcher(name.toString()).matches();
    }

    private static Path canonical(Path path) {
        try {
            return path.toRealPath();
        } catch (IOException e) {
            return path.toAbsolutePath().normalize();
        }
    }

    private static void logResolved(List<Path> resolved) {
        synchronized (LOG_LOCK) {
            if (Objects.equals(resolved, lastResolved)) {
                return;
            }
            lastResolved = resolved;
        }
        if (resolved.isEmpty()) {
            log.warn("connectors.json USB ids matched no serial device");
        } else {
            log.info("Resolved connectors.json USB ids to {}", resolved);
        }
    }

    private static List<DeviceSpec> load(Path connectorsJson) {
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

    private static List<DeviceSpec> parseArray(JsonNode array) {
        List<DeviceSpec> specs = new ArrayList<>();
        for (JsonNode entry : array) {
            if (!entry.isObject()) {
                log.warn("Skipping connectors.json entry {}", entry);
                continue;
            }
            String vendor = text(entry, "vendorId", "vendor", "idVendor");
            String product = text(entry, "deviceId", "productId", "product", "idProduct");
            if (vendor != null && product != null) {
                Optional<UsbId> id = UsbId.tryParse(vendor, product);
                if (id.isEmpty()) {
                    log.warn("Skipping connectors.json USB id {}:{}", vendor, product);
                    continue;
                }
                specs.add(new VendorProduct(id.get()));
                continue;
            }
            String path = text(entry, "path");
            if (path != null) {
                specs.add(new ExplicitPath(path));
                continue;
            }
            log.warn("Skipping connectors.json entry {}", entry);
        }
        return List.copyOf(specs);
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

    private static List<DeviceSpec> arduinoMegaDefaults() {
        return List.of(new VendorProduct(new UsbId("2341", "0042")), new VendorProduct(new UsbId("2a03", "0042")));
    }

    private interface DeviceSpec {
        List<Path> resolve(Path sysClassTty, Path devRoot);
    }

    private record VendorProduct(UsbId id) implements DeviceSpec {
        @Override
        public List<Path> resolve(Path sysClassTty, Path devRoot) {
            if (!Files.isDirectory(sysClassTty)) {
                return List.of();
            }
            try (Stream<Path> entries = Files.list(sysClassTty)) {
                return entries.map(path -> path.getFileName().toString())
                        .filter(name -> TTY_NAME.matcher(name).matches())
                        .filter(name -> Files.exists(devRoot.resolve(name)))
                        .filter(name -> id.equals(usbIdOf(sysClassTty.resolve(name)).orElse(null)))
                        .map(devRoot::resolve)
                        .toList();
            } catch (IOException e) {
                log.warn("Failed to scan {} for USB id {}", sysClassTty, id, e);
                return List.of();
            }
        }
    }

    private record ExplicitPath(String path) implements DeviceSpec {
        @Override
        public List<Path> resolve(Path sysClassTty, Path devRoot) {
            return List.of(Path.of(path));
        }
    }

    private static Optional<UsbId> usbIdOf(Path sysTtyDir) {
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
                    return UsbId.tryParse(Files.readString(vendor).trim(), Files.readString(product).trim());
                }
                current = current.getParent();
            }
        } catch (IOException e) {
            return Optional.empty();
        }
        return Optional.empty();
    }

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
