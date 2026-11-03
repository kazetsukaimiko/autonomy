package io.freedriver.autonomy.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import io.freedriver.jsonlink.config.v2.Mapping;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class MappingsDirectoryLoaderTest {
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
    void absentDirectoryLeavesTheV2File() throws Exception {
        UUID board = UUID.randomUUID();
        Path v2 = writeV2(boardFile(board, "from-v2"));
        assertFalse(MappingsDirectoryLoader.directoryPresent(v2));
    }

    @Test
    void presentDirectoryDoesNotLoadV2() throws Exception {
        UUID v2Board = UUID.randomUUID();
        UUID v3Board = UUID.randomUUID();
        Path v2 = writeV2(boardFile(v2Board, "from-v2"));
        Path dir = mappingsDir(v2);
        writeBoard(dir, "a.json", v3Board, "from-v3");
        MappingsDirectoryLoader.Result result = MappingsDirectoryLoader.load(v2, uid(dir));
        Set<UUID> ids = result.mappings().getMappings().stream()
                .map(Mapping::connectorId)
                .collect(Collectors.toSet());
        assertEquals(Set.of(v3Board), ids);
        assertTrue(result.usedDirectory());
        assertEquals("from-v3", result.mappings().getMappings().iterator().next().connectorName());
    }

    @Test
    void firstBoardIdWinsInSortedFileNameOrder() throws Exception {
        UUID board = UUID.randomUUID();
        Path v2 = writeV2(boardFile(UUID.randomUUID(), "v2"));
        Path dir = mappingsDir(v2);
        writeBoard(dir, "b-later.json", board, "second");
        writeBoard(dir, "a-first.json", board, "first");
        MappingsDirectoryLoader.Result result = MappingsDirectoryLoader.load(v2, uid(dir));
        assertEquals("first", result.mappings().getMappings().iterator().next().connectorName());
        assertTrue(result.logs().contains(MappingsDirectoryLoader.ALREADY_DECLARED));
    }

    @Test
    void invalidFirstFileRefusesTheBoard() throws Exception {
        UUID board = UUID.randomUUID();
        Path v2 = writeV2(boardFile(UUID.randomUUID(), "v2"));
        Path dir = mappingsDir(v2);
        Path first = writeBoard(dir, "a.json", board, "bad");
        var permissions = EnumSet.copyOf(Files.getPosixFilePermissions(first));
        permissions.add(PosixFilePermission.GROUP_WRITE);
        Files.setPosixFilePermissions(first, permissions);
        writeBoard(dir, "b.json", board, "good");
        MappingsDirectoryLoader.Result result = MappingsDirectoryLoader.load(v2, uid(dir));
        assertTrue(result.mappings().getMappings().isEmpty());
        assertTrue(result.logs().contains(MappingsDirectoryLoader.ALREADY_DECLARED));
        assertTrue(result.errors().stream().anyMatch(error -> error.contains("writable by group")));
    }

    @Test
    void aBadFileDoesNotRefuseOtherBoards() throws Exception {
        UUID good = UUID.randomUUID();
        UUID bad = UUID.randomUUID();
        Path v2 = writeV2(boardFile(UUID.randomUUID(), "v2"));
        Path dir = mappingsDir(v2);
        writeBoard(dir, "a.json", good, "good");
        Path other = dir.resolve("b.json");
        Files.writeString(other, "{not-json");
        ownerOnlyFile(other);
        MappingsDirectoryLoader.Result result = MappingsDirectoryLoader.load(v2, uid(dir));
        assertEquals(Set.of(good), result.mappings().getMappings().stream()
                .map(Mapping::connectorId)
                .collect(Collectors.toSet()));
        assertTrue(result.errors().stream().anyMatch(error -> error.contains("not valid JSON")));
        assertEquals("good", result.mappings().getMappings().iterator().next().connectorName());
    }

    private Path writeV2(String json) throws IOException {
        Path dir = Files.createTempDirectory("mappings-v3");
        ownerOnlyDirectory(dir);
        trash.add(dir);
        Path file = dir.resolve("mappings_v2.json");
        Files.writeString(file, json);
        ownerOnlyFile(file);
        return file;
    }

    private Path mappingsDir(Path v2) throws IOException {
        Path dir = v2.getParent().resolve("mappings.d");
        Files.createDirectory(dir);
        ownerOnlyDirectory(dir);
        trash.add(dir);
        return dir;
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

    private static long uid(Path path) throws IOException {
        return ConfigFileTrust.uid(path);
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
