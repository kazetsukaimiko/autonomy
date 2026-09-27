package io.freedriver.autonomy.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import io.freedriver.jsonlink.config.ConfigMapper;
import io.freedriver.jsonlink.config.ConnectorConfig;
import io.freedriver.jsonlink.config.DeviceByPath;
import io.freedriver.jsonlink.config.DeviceByVendorAndDeviceId;
import io.freedriver.jsonlink.config.DevicePathSupplier;
import io.freedriver.serial.connection.LinuxUsbSysfsDiscovery;
import lombok.extern.slf4j.Slf4j;

/**
 * Board ports from jsonlink's polymorphic {@link DevicePathSupplier} list in
 * {@code ~/.config/jsonlink/connectors.json}.
 *
 * <p>Jackson deduction selects {@link DeviceByVendorAndDeviceId} or {@link DeviceByPath}.
 * Vendor entries are resolved with {@link LinuxUsbSysfsDiscovery}. Only {@code ttyACM}
 * nodes are opened, and one canonical path is returned once.
 */
@Slf4j
public final class BoardDeviceDiscovery {
    private static final Path CONFIG_FILE =
            Path.of(System.getProperty("user.home"), ".config", "jsonlink", "connectors.json");
    private static final Path SYS_CLASS_TTY = Path.of("/sys/class/tty");
    private static final Path DEV_ROOT = Path.of("/dev");
    private static final Pattern TTY_ACM = Pattern.compile("ttyACM\\d+");
    private static final TypeReference<List<DevicePathSupplier>> DEVICE_LIST = new TypeReference<>() {};
    private static final Object LOG_LOCK = new Object();
    private static List<Path> lastResolved;

    private BoardDeviceDiscovery() {}

    public static List<Path> discover() {
        return discover(CONFIG_FILE, SYS_CLASS_TTY, DEV_ROOT);
    }

    static List<Path> discover(Path connectorsJson, Path sysClassTty, Path devRoot) {
        LinkedHashMap<Path, Path> unique = new LinkedHashMap<>();
        for (DevicePathSupplier supplier : load(connectorsJson)) {
            for (Path path : expand(supplier, sysClassTty, devRoot)) {
                unique.putIfAbsent(canonical(path), path.toAbsolutePath().normalize());
            }
        }
        return publish(List.copyOf(unique.values()));
    }

    static boolean isTtyAcmNode(Path device) {
        Path name = device == null ? null : device.getFileName();
        return name != null && TTY_ACM.matcher(name.toString()).matches();
    }

    private static List<Path> expand(DevicePathSupplier supplier, Path sysClassTty, Path devRoot) {
        if (supplier instanceof DeviceByVendorAndDeviceId usb) {
            return LinuxUsbSysfsDiscovery.discover(sysClassTty, devRoot, List.of(usb.usbId())).stream()
                    .filter(BoardDeviceDiscovery::isTtyAcmNode)
                    .toList();
        }
        if (supplier instanceof DeviceByPath byPath) {
            Path path = Path.of(byPath.path());
            if (isTtyAcmNode(path)) {
                return List.of(path);
            }
            log.warn("Skipping device path {} because it is not a ttyACM node", path);
            return List.of();
        }
        log.warn("Skipping unsupported device supplier {}", supplier);
        return List.of();
    }

    private static List<DevicePathSupplier> load(Path connectorsJson) {
        if (connectorsJson == null || !Files.isRegularFile(connectorsJson)) {
            log.warn("connectors.json missing at {}; using Arduino Mega USB id defaults", connectorsJson);
            return ConnectorConfig.defaultDevices();
        }
        try {
            JsonNode node = ConfigMapper.MAPPER.readTree(connectorsJson.toFile());
            if (!node.isArray()) {
                log.warn(
                        "connectors.json at {} is not a device list; using Arduino Mega USB id defaults",
                        connectorsJson);
                return ConnectorConfig.defaultDevices();
            }
            List<DevicePathSupplier> devices = ConfigMapper.MAPPER.convertValue(node, DEVICE_LIST);
            return devices == null ? ConnectorConfig.defaultDevices() : List.copyOf(devices);
        } catch (IOException | IllegalArgumentException e) {
            log.warn("Couldn't read connectors.json at {}; using Arduino Mega USB id defaults", connectorsJson, e);
            return ConnectorConfig.defaultDevices();
        }
    }

    private static Path canonical(Path path) {
        try {
            return path.toRealPath();
        } catch (IOException e) {
            return path.toAbsolutePath().normalize();
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
}
