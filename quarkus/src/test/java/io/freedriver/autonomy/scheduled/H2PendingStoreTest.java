package io.freedriver.autonomy.scheduled;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class H2PendingStoreTest {

    @TempDir
    Path temp;

    @Test
    void rowsAndAppliedListIdsSurviveAReopen() throws Exception {
        Path dir = temp.resolve("store");
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID listId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Instant due = Instant.parse("2026-11-22T18:00:30Z");
        try (H2PendingStore store = H2PendingStore.open(dir).orElseThrow()) {
            store.admit(listId, Instant.parse("2026-11-22T18:00:00Z"), Set.of(), Set.of(),
                    List.of(new PendingRow(id, "lamp", false, due)));
        }
        try (H2PendingStore store = H2PendingStore.open(dir).orElseThrow()) {
            assertEquals(List.of(new PendingRow(id, "lamp", false, due)), store.pending());
            assertTrue(store.seen(listId, Instant.parse("2026-11-22T18:00:10Z")));
        }
    }

    @Test
    void aGroupWritableDirectoryStaysClosed() throws Exception {
        Path dir = temp.resolve("open");
        Files.createDirectory(dir);
        Files.setPosixFilePermissions(dir, EnumSet.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE,
                PosixFilePermission.GROUP_WRITE));
        assertEquals(Optional.empty(), H2PendingStore.open(dir));
    }

    @Test
    void aSymlinkStaysClosed() throws Exception {
        Path real = temp.resolve("real");
        Files.createDirectory(real);
        Path link = temp.resolve("link");
        Files.createSymbolicLink(link, real);
        assertEquals(Optional.empty(), H2PendingStore.open(link));
    }
}
