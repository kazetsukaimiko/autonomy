package io.freedriver.autonomy.config;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.freedriver.autonomy.jaxrs.ObjectMapperContextResolver;
import io.freedriver.jsonlink.config.ConnectorConfig;
import io.freedriver.jsonlink.config.DevicePathSupplier;
import lombok.extern.slf4j.Slf4j;

/**
 * Loads split connector files from {@code connections.d/} at startup.
 * When that directory is absent, callers keep {@code connectors.json}.
 */
@Slf4j
public final class ConnectionsDirectoryLoader {
    public static final String DIRECTORY_NAME = "connections.d";
    private static final TypeReference<List<DevicePathSupplier>> DEVICE_LIST = new TypeReference<>() {};

    private ConnectionsDirectoryLoader() {}

    public static boolean directoryPresent(Path dir) {
        return dir != null && Files.exists(dir, LinkOption.NOFOLLOW_LINKS);
    }

    public static Result load(Path dir, long processUid) {
        List<String> errors = new ArrayList<>();
        if (dir == null) {
            errors.add("connections.d path is missing");
            return new Result(new ConnectorConfig(List.of()), errors, true);
        }
        List<String> dirProblems = ConfigFileTrust.nodeProblems(dir, "connections.d directory", processUid);
        if (!Files.isDirectory(dir, LinkOption.NOFOLLOW_LINKS) || !dirProblems.isEmpty()) {
            dirProblems.forEach(error -> {
                log.error(error);
                errors.add(error);
            });
            if (dirProblems.isEmpty()) {
                String message = "connections.d is not a directory: " + dir;
                log.error(message);
                errors.add(message);
            }
            return new Result(new ConnectorConfig(List.of()), errors, true);
        }

        List<DevicePathSupplier> devices = new ArrayList<>();
        Set<DevicePathSupplier> seen = new LinkedHashSet<>();
        ObjectMapper mapper = ObjectMapperContextResolver.getMapper();
        for (Path file : sortedFiles(dir, errors)) {
            List<String> trust = ConfigFileTrust.nodeProblems(file, "connections file", processUid);
            if (!trust.isEmpty()) {
                trust.forEach(error -> {
                    log.error(error);
                    errors.add(error);
                });
                continue;
            }
            try {
                JsonNode node = mapper.readTree(file.toFile());
                List<DevicePathSupplier> parsed = parseDevices(mapper, node);
                for (DevicePathSupplier device : parsed) {
                    if (!seen.add(device)) {
                        continue;
                    }
                    devices.add(device);
                }
            } catch (IOException | IllegalArgumentException e) {
                String message = "connections file could not be read: " + file + " (" + e.getMessage() + ")";
                log.error(message);
                errors.add(message);
            }
        }
        return new Result(new ConnectorConfig(devices), errors, true);
    }

    private static List<DevicePathSupplier> parseDevices(ObjectMapper mapper, JsonNode node) {
        if (node == null || node.isNull()) {
            return List.of();
        }
        if (node.isArray()) {
            return mapper.convertValue(node, DEVICE_LIST);
        }
        return List.of(mapper.convertValue(node, DevicePathSupplier.class));
    }

    private static List<Path> sortedFiles(Path dir, List<String> errors) {
        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path path : stream) {
                if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
                    continue;
                }
                files.add(path);
            }
        } catch (IOException e) {
            String message = "connections.d could not be listed: " + dir + " (" + e.getMessage() + ")";
            log.error(message);
            errors.add(message);
            return List.of();
        }
        files.sort(Comparator.comparing(path -> path.getFileName().toString()));
        return files;
    }

    public record Result(ConnectorConfig config, List<String> errors, boolean usedDirectory) {
        public Result {
            config = config == null ? new ConnectorConfig(List.of()) : config;
            errors = errors == null ? List.of() : List.copyOf(errors);
        }
    }
}
