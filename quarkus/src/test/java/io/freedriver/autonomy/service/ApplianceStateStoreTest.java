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
    void createsMissingParentsWithDefaultPermissionsAndTheStateDirectoryAs0700() throws IOException {
        assumeTrue(posix(), "POSIX file permissions are not available");
        Path parent = temp.resolve("missing-parent");
        Path directory = parent.resolve("state");
        Path controlParent = temp.resolve("control-parent");
        Files.createDirectories(controlParent);
        String defaultMode = PosixFilePermissions.toString(Files.getPosixFilePermissions(controlParent));

        ApplianceStateStore store = new ApplianceStateStore(directory);
        store.write(Map.of("fridge", true));

        assertEquals(defaultMode, PosixFilePermissions.toString(Files.getPosixFilePermissions(parent)));
        assertEquals("rwx------", PosixFilePermissions.toString(Files.getPosixFilePermissions(directory)));
    }

    @Test
    void movesAnUnreadableFileAsideBeforeWritingANewOne() throws IOException {
        Path directory = temp.resolve("state");
        Files.createDirectories(directory);
        Path file = directory.resolve(ApplianceStateStore.FILE_NAME);
        Files.writeString(file, "{");
        ApplianceStateStore store = new ApplianceStateStore(directory);

        java.util.List<String> lines = capture(() -> {
            try {
                store.merge(Map.of("fridge", true));
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        });

        Path corrupt = directory.resolve(ApplianceStateStore.CORRUPT_FILE_NAME);
        assertEquals("{", Files.readString(corrupt));
        assertEquals(Boolean.TRUE, store.load().states().get("fridge"));
        assertTrue(lines.stream().anyMatch(line -> line.contains("aside to appliance-state.json.corrupt")), lines::toString);

        Files.writeString(file, "{");
        store.merge(Map.of("fridge", false));
        assertEquals("{", Files.readString(corrupt));
        assertEquals(Boolean.FALSE, store.load().states().get("fridge"));
    }

    @Test
    void refusesASymlinkedStateDirectory() throws IOException {
        assumeTrue(posix(), "POSIX file permissions are not available");
        Path real = temp.resolve("real");
        Files.createDirectories(real);
        Path link = temp.resolve("link");
        Files.createSymbolicLink(link, real);
        ApplianceStateStore store = new ApplianceStateStore(link);

        java.util.List<String> lines = capture(() -> {
            try {
                store.write(Map.of("fridge", true));
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        });

        assertEquals(LoadedApplianceState.Status.UNTRUSTED, store.load().status());
        assertFalse(Files.exists(real.resolve(ApplianceStateStore.FILE_NAME)));
        assertTrue(lines.stream().anyMatch(line -> line.contains("not a real directory")), lines::toString);
    }

    @Test
    void refusesASymlinkedStateFile() throws IOException {
        assumeTrue(posix(), "POSIX file permissions are not available");
        Path directory = temp.resolve("state");
        Files.createDirectories(directory);
        Path outside = temp.resolve("outside.json");
        Files.writeString(outside, "{\"fridge\":true}");
        Path link = directory.resolve(ApplianceStateStore.FILE_NAME);
        Files.createSymbolicLink(link, outside);
        ApplianceStateStore store = new ApplianceStateStore(directory);

        java.util.List<String> lines = capture(store::load);

        assertEquals(LoadedApplianceState.Status.UNTRUSTED, store.load().status());
        assertEquals("{\"fridge\":true}", Files.readString(outside));
        assertTrue(Files.isSymbolicLink(link));
        store.merge(Map.of("hallway", true));
        assertEquals("{\"fridge\":true}", Files.readString(outside));
        assertTrue(Files.isSymbolicLink(link));
        assertTrue(lines.stream().anyMatch(line -> line.contains("not a regular file")), lines::toString);
    }

    @Test
    void refusesAGroupWritableStateFile() throws IOException {
        assumeTrue(posix(), "POSIX file permissions are not available");
        Path directory = temp.resolve("state");
        ApplianceStateStore store = new ApplianceStateStore(directory);
        store.write(Map.of("fridge", true));
        Files.setPosixFilePermissions(
                store.stateFile(), PosixFilePermissions.fromString("rw-rw----"));

        java.util.List<String> lines = capture(store::load);

        assertEquals(LoadedApplianceState.Status.UNTRUSTED, store.load().status());
        assertTrue(store.load().states().isEmpty());
        store.merge(Map.of("fridge", false));
        assertTrue(Files.readString(store.stateFile()).contains("true"));
        assertEquals(
                "rw-rw----",
                PosixFilePermissions.toString(Files.getPosixFilePermissions(store.stateFile())));
        assertTrue(lines.stream().anyMatch(line -> line.contains("group or other writable")), lines::toString);
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
