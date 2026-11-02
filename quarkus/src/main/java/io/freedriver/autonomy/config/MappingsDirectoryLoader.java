package io.freedriver.autonomy.config;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.freedriver.autonomy.jaxrs.ObjectMapperContextResolver;
import io.freedriver.jsonlink.config.v2.Mapping;
import io.freedriver.jsonlink.config.v2.Mappings;
import io.freedriver.jsonlink.config.v2.MappingsAction;
import lombok.extern.slf4j.Slf4j;

/**
 * Loads per-board mapping files from {@code mappings.d/} at startup.
 * When that directory is absent, callers keep the v2 mappings file.
 */
@Slf4j
public final class MappingsDirectoryLoader {
    public static final String DIRECTORY_NAME = "mappings.d";
    public static final String ALREADY_DECLARED = "Board already declared, ignoring";

    private MappingsDirectoryLoader() {}

    public static Path directory(Path mappingsFile) {
        Path parent = mappingsFile == null ? null : mappingsFile.getParent();
        return parent == null ? null : parent.resolve(DIRECTORY_NAME);
    }

    public static boolean directoryPresent(Path mappingsFile) {
        Path dir = directory(mappingsFile);
        return dir != null && Files.exists(dir, LinkOption.NOFOLLOW_LINKS);
    }

    public static Result load(Path mappingsFile, long processUid) {
        Path dir = directory(mappingsFile);
        List<String> errors = new ArrayList<>();
        List<String> logs = new ArrayList<>();
        if (dir == null) {
            errors.add("mappings.d has no parent directory");
            return new Result(emptyMappings(), errors, logs, true, true);
        }
        List<String> dirProblems = ConfigFileTrust.nodeProblems(dir, "mappings.d directory", processUid);
        if (!Files.isDirectory(dir, LinkOption.NOFOLLOW_LINKS) || !dirProblems.isEmpty()) {
            dirProblems.forEach(error -> {
                log.error(error);
                errors.add(error);
            });
            if (dirProblems.isEmpty()) {
                String message = "mappings.d is not a directory: " + dir;
                log.error(message);
                errors.add(message);
            }
            return new Result(emptyMappings(), errors, logs, true, true);
        }

        Map<UUID, Mapping> loaded = new LinkedHashMap<>();
        Set<UUID> declared = new LinkedHashSet<>();
        Map<String, List<MappingsAction>> startupActions = new LinkedHashMap<>();
        Integer eventTTL = null;
        ChronoUnit eventTTLUnit = null;

        for (Path file : sortedFiles(dir, errors)) {
            FileLoad fileLoad = loadFile(file, processUid);
            errors.addAll(fileLoad.errors);
            logs.addAll(fileLoad.logs);
            for (UUID boardId : fileLoad.boardIds) {
                if (!declared.add(boardId)) {
                    log.info(ALREADY_DECLARED);
                    logs.add(ALREADY_DECLARED);
                    continue;
                }
                if (fileLoad.refused.contains(boardId)) {
                    continue;
                }
                Mapping mapping = fileLoad.mappings.get(boardId);
                if (mapping != null) {
                    loaded.put(boardId, mapping);
                }
            }
            fileLoad.startupActions.forEach((board, actions) -> {
                UUID boardId = parseUuid(board);
                Mapping mapping = boardId == null ? null : fileLoad.mappings.get(boardId);
                if (mapping == null || loaded.get(boardId) != mapping) {
                    return;
                }
                startupActions.putIfAbsent(board, actions);
            });
            if (eventTTL == null && fileLoad.eventTTL != null
                    && fileLoad.mappings.values().stream()
                            .anyMatch(mapping -> loaded.get(mapping.connectorId()) == mapping)) {
                eventTTL = fileLoad.eventTTL;
                eventTTLUnit = fileLoad.eventTTLUnit;
            }
        }

        Mappings mappings = new Mappings(
                eventTTL,
                eventTTLUnit,
                new LinkedHashSet<>(loaded.values()),
                startupActions);
        return new Result(mappings, errors, logs, true, false);
    }

    private static FileLoad loadFile(Path file, long processUid) {
        List<String> errors = new ArrayList<>();
        List<String> logs = new ArrayList<>();
        List<String> trust = ConfigFileTrust.nodeProblems(file, "mappings file", processUid);
        JsonNode node = readTree(file);
        List<UUID> boardIds = boardIds(node);
        if (!trust.isEmpty()) {
            trust.forEach(error -> {
                log.error(error);
                errors.add(error);
            });
            return FileLoad.refused(boardIds, errors, logs);
        }
        if (node == null) {
            String message = "mappings file is not valid JSON: " + file;
            log.error(message);
            errors.add(message);
            return FileLoad.refused(boardIds, errors, logs);
        }
        try {
            ObjectMapper mapper = ObjectMapperContextResolver.getMapper();
            Mappings parsed = parseMappings(mapper, node);
            Map<UUID, Mapping> byId = new LinkedHashMap<>();
            Set<UUID> refused = new LinkedHashSet<>();
            for (Mapping mapping : parsed.getMappings()) {
                UUID boardId = mapping.connectorId();
                if (boardId == null) {
                    String message = "mappings file has a mapping with no connectorId: " + file;
                    log.error(message);
                    errors.add(message);
                    continue;
                }
                byId.put(boardId, mapping);
            }
            if (byId.isEmpty() && boardIds.isEmpty()) {
                String message = "mappings file declares no board id: " + file;
                log.error(message);
                errors.add(message);
            }
            return new FileLoad(
                    List.copyOf(byId.keySet().isEmpty() ? boardIds : byId.keySet()),
                    byId,
                    refused,
                    parsed.getStartupActions(),
                    parsed.getEventTTL(),
                    parsed.getEventTTLUnit(),
                    errors,
                    logs);
        } catch (IOException | IllegalArgumentException | IllegalStateException e) {
            String message = "mappings file could not be read: " + file + " (" + e.getMessage() + ")";
            log.error(message);
            errors.add(message);
            return FileLoad.refused(boardIds, errors, logs);
        }
    }

    private static Mappings parseMappings(ObjectMapper mapper, JsonNode node) throws IOException {
        if (node.has("mappings") || node.has("startupActions") || node.has("eventTTL")) {
            return mapper.treeToValue(node, Mappings.class);
        }
        Mapping mapping = mapper.treeToValue(node, Mapping.class);
        Map<String, List<MappingsAction>> startup = Map.of();
        if (mapping.connectorId() != null && node.has("startupActions") && node.get("startupActions").isArray()) {
            List<MappingsAction> actions = mapper.convertValue(
                    node.get("startupActions"),
                    mapper.getTypeFactory().constructCollectionType(List.class, MappingsAction.class));
            startup = Map.of(mapping.connectorId().toString(), actions);
        }
        return new Mappings(null, null, Set.of(mapping), startup);
    }

    private static JsonNode readTree(Path file) {
        try {
            if (Files.isSymbolicLink(file)) {
                return null;
            }
            return ObjectMapperContextResolver.getMapper().readTree(file.toFile());
        } catch (IOException e) {
            return null;
        }
    }

    private static List<UUID> boardIds(JsonNode node) {
        if (node == null) {
            return List.of();
        }
        List<UUID> ids = new ArrayList<>();
        if (node.has("mappings") && node.get("mappings").isArray()) {
            for (JsonNode mapping : node.get("mappings")) {
                UUID id = uuid(mapping.get("connectorId"));
                if (id != null) {
                    ids.add(id);
                }
            }
            return ids;
        }
        UUID id = uuid(node.get("connectorId"));
        if (id != null) {
            ids.add(id);
        }
        return ids;
    }

    private static UUID uuid(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return parseUuid(node.asText());
    }

    private static UUID parseUuid(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(text);
        } catch (IllegalArgumentException e) {
            return null;
        }
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
            String message = "mappings.d could not be listed: " + dir + " (" + e.getMessage() + ")";
            log.error(message);
            errors.add(message);
            return List.of();
        }
        files.sort(Comparator.comparing(path -> path.getFileName().toString()));
        return files;
    }

    private static Mappings emptyMappings() {
        return new Mappings(null, null, Set.of(), Map.of());
    }

    public record Result(
            Mappings mappings,
            List<String> errors,
            List<String> logs,
            boolean usedDirectory,
            boolean directoryRefused) {
        public Result {
            mappings = mappings == null ? emptyMappings() : mappings;
            errors = errors == null ? List.of() : List.copyOf(errors);
            logs = logs == null ? List.of() : List.copyOf(logs);
        }
    }

    private record FileLoad(
            List<UUID> boardIds,
            Map<UUID, Mapping> mappings,
            Set<UUID> refused,
            Map<String, List<MappingsAction>> startupActions,
            Integer eventTTL,
            ChronoUnit eventTTLUnit,
            List<String> errors,
            List<String> logs) {
        static FileLoad refused(List<UUID> boardIds, List<String> errors, List<String> logs) {
            return new FileLoad(
                    boardIds,
                    Map.of(),
                    new LinkedHashSet<>(boardIds),
                    Map.of(),
                    null,
                    null,
                    errors,
                    logs);
        }
    }
}
