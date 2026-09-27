package io.freedriver.autonomy.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ApplianceStateStoreTest {

    @TempDir
    Path temp;

    @Test
    void writesAtomicallyInTheStateDirectory() throws IOException {
        Path directory = temp.resolve("state");
        Path[] tempParent = new Path[1];
        String[] tempMode = new String[1];
        ApplianceStateStore store = new ApplianceStateStore(directory) {
            @Override
            void moveIntoPlace(Path tempFile, Path target) throws IOException {
                tempParent[0] = tempFile.getParent();
                if (posix()) {
                    tempMode[0] = PosixFilePermissions.toString(Files.getPosixFilePermissions(tempFile));
                }
                super.moveIntoPlace(tempFile, target);
            }
        };

        store.write(Map.of("fridge", true, "hallway", false));

        assertEquals(directory, tempParent[0]);
        assertEquals(directory, store.stateFile().getParent());
        LoadedApplianceState loaded = store.load();
        assertEquals(LoadedApplianceState.Status.PRESENT, loaded.status());
        assertEquals(Boolean.TRUE, loaded.states().get("fridge"));
        assertEquals(Boolean.FALSE, loaded.states().get("hallway"));
        assertNoTempFiles(directory);
        if (posix()) {
            assertEquals("rw-------", tempMode[0]);
        }
    }

    @Test
    void leavesThePreviousFileWhenTheRenameFails() throws IOException {
        Path directory = temp.resolve("state");
        ApplianceStateStore store = new ApplianceStateStore(directory);
        store.write(Map.of("fridge", true));

        ApplianceStateStore failing = new ApplianceStateStore(directory) {
            @Override
            void moveIntoPlace(Path tempFile, Path target) throws IOException {
                throw new IOException("simulated failure");
            }
        };

        assertThrows(IOException.class, () -> failing.write(Map.of("fridge", false)));
        assertEquals(Boolean.TRUE, store.load().states().get("fridge"));
        assertNoTempFiles(directory);
    }

    @Test
    void directoryIs0700AndFileIs0600() throws IOException {
        assumeTrue(posix(), "POSIX file permissions are not available");
        Path directory = temp.resolve("state");
        ApplianceStateStore store = new ApplianceStateStore(directory);
        store.write(Map.of("fridge", true));

        assertEquals("rwx------", PosixFilePermissions.toString(Files.getPosixFilePermissions(directory)));
        assertEquals("rw-------", PosixFilePermissions.toString(Files.getPosixFilePermissions(store.stateFile())));
    }

    @Test
    void tightensADirectoryThatGroupOrOthersCanRead() throws IOException {
        assumeTrue(posix(), "POSIX file permissions are not available");
        Path directory = temp.resolve("state");
        Files.createDirectories(directory);
        Files.setPosixFilePermissions(directory, PosixFilePermissions.fromString("rwxr-xr-x"));
        ApplianceStateStore store = new ApplianceStateStore(directory);

        java.util.List<String> lines = capture(() -> {
            try {
                store.write(Map.of("fridge", true));
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        });

        assertEquals("rwx------", PosixFilePermissions.toString(Files.getPosixFilePermissions(directory)));
        assertTrue(lines.stream().anyMatch(line -> line.contains("tightening it to 0700")), lines::toString);
    }

    @Test
    void skipsEntriesThatDoNotParseAndTreatsACorruptFileAsMissing() throws IOException {
        Path directory = temp.resolve("state");
        Files.createDirectories(directory);
        Path file = directory.resolve(ApplianceStateStore.FILE_NAME);
        Files.writeString(file, """
                {"fridge":true,"hallway":"sometimes","note":1}
                """);
        ApplianceStateStore store = new ApplianceStateStore(directory);

        java.util.List<String> skipped = capture(store::load);
        LoadedApplianceState loaded = store.load();
        assertEquals(LoadedApplianceState.Status.PRESENT, loaded.status());
        assertEquals(Map.of("fridge", true), loaded.states());
        assertTrue(skipped.stream().anyMatch(line -> line.contains("Skipping appliance state entry 'hallway'")));
        assertTrue(skipped.stream().anyMatch(line -> line.contains("Skipping appliance state entry 'note'")));

        Files.writeString(file, "{");
        java.util.List<String> corrupt = capture(store::load);
        LoadedApplianceState unreadable = store.load();
        assertEquals(LoadedApplianceState.Status.UNREADABLE, unreadable.status());
        assertTrue(unreadable.states().isEmpty());
        assertTrue(corrupt.stream().anyMatch(line -> line.contains("treating it as no saved state")), corrupt::toString);
    }

    @Test
    void missingFileIsAFirstStart() {
        ApplianceStateStore store = new ApplianceStateStore(temp.resolve("absent"));
        LoadedApplianceState loaded = store.load();
        assertEquals(LoadedApplianceState.Status.MISSING, loaded.status());
        assertTrue(loaded.states().isEmpty());
    }

    private static void assertNoTempFiles(Path directory) throws IOException {
        try (Stream<Path> listing = Files.list(directory)) {
            assertFalse(listing.anyMatch(path -> path.getFileName().toString().contains(".tmp")));
        }
    }

    private static boolean posix() {
        return FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
    }

    private static java.util.List<String> capture(Runnable action) {
        Logger logger = Logger.getLogger(ApplianceStateStore.class.getName());
        Level previous = logger.getLevel();
        logger.setLevel(Level.ALL);
        java.util.List<String> lines = new java.util.ArrayList<>();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                if (record != null && record.getMessage() != null) {
                    lines.add(LogMessages.format(record));
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        handler.setLevel(Level.ALL);
        logger.addHandler(handler);
        try {
            action.run();
        } finally {
            logger.removeHandler(handler);
            logger.setLevel(previous);
        }
        return lines;
    }
}
