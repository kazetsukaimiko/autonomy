package io.freedriver.autonomy.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import io.freedriver.jsonlink.config.DeviceByPath;
import io.freedriver.jsonlink.config.DevicePathSupplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ConnectionsDirectoryLoaderTest {
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
    void splitFilesMergeInSortedNameOrder() throws Exception {
        Path dir = directory();
        write(dir, "b.json", "[{\"path\":\"/dev/ttyUSB1\"}]");
        write(dir, "a.json", "[{\"path\":\"/dev/ttyUSB0\"}]");
        ConnectionsDirectoryLoader.Result result = ConnectionsDirectoryLoader.load(dir, ConfigFileTrust.uid(dir));
        assertEquals(
                List.of(new DeviceByPath("/dev/ttyUSB0"), new DeviceByPath("/dev/ttyUSB1")),
                result.config().devices());
    }

    @Test
    void firstDuplicateWinsAndABadFileLeavesTheOthers() throws Exception {
        Path dir = directory();
        write(dir, "a.json", "[{\"path\":\"/dev/ttyUSB0\"}]");
        write(dir, "b.json", "{not-json");
        write(dir, "c.json", "{\"path\":\"/dev/ttyUSB0\"}");
        ConnectionsDirectoryLoader.Result result = ConnectionsDirectoryLoader.load(dir, ConfigFileTrust.uid(dir));
        List<DevicePathSupplier> devices = result.config().devices();
        assertEquals(List.of(new DeviceByPath("/dev/ttyUSB0")), devices);
        assertTrue(result.errors().stream().anyMatch(error -> error.contains("could not be read")));
    }

    @Test
    void groupWritableFileIsSkipped() throws Exception {
        Path dir = directory();
        Path bad = write(dir, "a.json", "[{\"path\":\"/dev/ttyUSB0\"}]");
        var permissions = EnumSet.copyOf(Files.getPosixFilePermissions(bad));
        permissions.add(PosixFilePermission.GROUP_WRITE);
        Files.setPosixFilePermissions(bad, permissions);
        write(dir, "b.json", "[{\"path\":\"/dev/ttyUSB1\"}]");
        ConnectionsDirectoryLoader.Result result = ConnectionsDirectoryLoader.load(dir, ConfigFileTrust.uid(dir));
        assertEquals(List.of(new DeviceByPath("/dev/ttyUSB1")), result.config().devices());
        assertTrue(result.errors().stream().anyMatch(error -> error.contains("writable by group")));
    }

    private Path directory() throws IOException {
        Path dir = Files.createTempDirectory("connections-d");
        Files.setPosixFilePermissions(dir, EnumSet.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE));
        trash.add(dir);
        return dir;
    }

    private Path write(Path dir, String name, String json) throws IOException {
        Path file = dir.resolve(name);
        Files.writeString(file, json);
        Files.setPosixFilePermissions(file, EnumSet.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE));
        trash.add(file);
        return file;
    }
}
