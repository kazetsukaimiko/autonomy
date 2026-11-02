package io.freedriver.autonomy.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class MappingsV3WiringTest {
    private final List<Path> trash = new ArrayList<>();

    @AfterEach
    void cleanup() throws IOException {
        for (int i = trash.size() - 1; i >= 0; i--) {
            Path path = trash.get(i);
            if (Files.isDirectory(path) && !Files.isSymbolicLink(path)) {
                try (var stream = Files.list(path)) {
                    for (Path child : stream.toList()) {
                        Files.deleteIfExists(child);
                    }
                }
            }
            Files.deleteIfExists(path);
        }
    }

    @Test
    void v2FileLoadsWhenMappingsDIsAbsent() throws Exception {
        UUID board = UUID.randomUUID();
        Path v2 = writeV2(boardFile(board, "from-v2"));
        SimpleAliasService aliases = new SimpleAliasService();
        aliases.mappingsFile = v2.toString();
        assertEquals("from-v2", aliases.getMapping(board).connectorName());
    }

    @Test
    void directorySnapshotDoesNotReload() throws Exception {
        UUID board = UUID.randomUUID();
        Path v2 = writeV2(boardFile(UUID.randomUUID(), "v2"));
        Path dir = v2.getParent().resolve("mappings.d");
        Files.createDirectory(dir);
        ownerOnlyDirectory(dir);
        trash.add(dir);
        writeBoard(dir, "a.json", board, "first");
        SimpleAliasService aliases = new SimpleAliasService();
        aliases.mappingsFile = v2.toString();
        assertEquals("first", aliases.getMapping(board).connectorName());
        writeBoard(dir, "b.json", UUID.randomUUID(), "later-board");
        assertEquals(1, aliases.getMappings().getMappings().size());
        assertEquals("first", aliases.getMapping(board).connectorName());
    }

    private Path writeV2(String json) throws IOException {
        Path dir = Files.createTempDirectory("mappings-v3-wiring");
        ownerOnlyDirectory(dir);
        trash.add(dir);
        Path file = dir.resolve("mappings_v2.json");
        Files.writeString(file, json);
        ownerOnlyFile(file);
        return file;
    }

    private Path writeBoard(Path dir, String name, UUID board, String connectorName) throws IOException {
        Path file = dir.resolve(name);
        Files.writeString(file, boardFile(board, connectorName));
        ownerOnlyFile(file);
        trash.add(file);
        return file;
    }

    private static String boardFile(UUID board, String connectorName) {
        return """
                {
                  "eventTTL": 7,
                  "eventTTLUnit": "DAYS",
                  "mappings": [{
                    "connectorId": "%s",
                    "connectorName": "%s",
                    "appliances": [],
                    "controlMap": {}
                  }]
                }
                """.formatted(board, connectorName);
    }

    private static void ownerOnlyDirectory(Path dir) throws IOException {
        Files.setPosixFilePermissions(dir, EnumSet.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE));
    }

    private static void ownerOnlyFile(Path file) throws IOException {
        Files.setPosixFilePermissions(file, EnumSet.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE));
    }
}
