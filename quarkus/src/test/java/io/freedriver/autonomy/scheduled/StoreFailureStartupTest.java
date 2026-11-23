package io.freedriver.autonomy.scheduled;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import io.freedriver.autonomy.MappingsTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

/**
 * Each store failure leaves the process up: /rest/simple answers, and command
 * lists stay unsubscribed with no row armed.
 */
abstract class StoreFailureStartupTest {

    @Inject
    ScheduledCommands scheduled;

    @Test
    void commandsStayUpWhileTheStoreStaysClosed() {
        given().when().get("/rest/simple").then().statusCode(200);
        assertFalse(scheduled.available());
    }

    static Path freshDirectory(String name) throws IOException {
        Path dir = Path.of(System.getProperty("java.io.tmpdir"), name + "-" + UUID.randomUUID());
        Files.createDirectories(dir);
        ownerOnly(dir);
        return dir;
    }

    static void ownerOnly(Path path) throws IOException {
        Files.setPosixFilePermissions(path, EnumSet.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE));
    }

    static void relax(Path dir) throws IOException {
        if (dir == null || !Files.exists(dir, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        if (Files.isSymbolicLink(dir)) {
            Files.deleteIfExists(dir);
            return;
        }
        ownerOnly(dir);
        if (Files.isDirectory(dir)) {
            try (Stream<Path> files = Files.walk(dir)) {
                files.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        if (!Files.isSymbolicLink(path)) {
                            ownerOnly(path);
                        }
                        Files.deleteIfExists(path);
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                });
            }
        }
    }
}

@QuarkusTest
@QuarkusTestResource(MappingsTestResource.class)
@TestProfile(UnwritableStoreStartupTest.Profile.class)
class UnwritableStoreStartupTest extends StoreFailureStartupTest {

    @AfterAll
    static void cleanup() throws IOException {
        StoreFailureStartupTest.relax(Profile.DIR);
    }

    public static class Profile implements QuarkusTestProfile {
        static final Path DIR = directory();

        private static Path directory() {
            try {
                Path dir = StoreFailureStartupTest.freshDirectory("autonomy-scheduled-unwritable");
                Files.setPosixFilePermissions(dir, EnumSet.of(
                        PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_EXECUTE));
                return dir;
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("autonomy.scheduled.store-dir", DIR.toString());
        }
    }
}

@QuarkusTest
@QuarkusTestResource(MappingsTestResource.class)
@TestProfile(GroupWritableStoreStartupTest.Profile.class)
class GroupWritableStoreStartupTest extends StoreFailureStartupTest {

    @AfterAll
    static void cleanup() throws IOException {
        StoreFailureStartupTest.relax(Profile.DIR);
    }

    public static class Profile implements QuarkusTestProfile {
        static final Path DIR = directory();

        private static Path directory() {
            try {
                Path dir = StoreFailureStartupTest.freshDirectory("autonomy-scheduled-group");
                Files.setPosixFilePermissions(dir, EnumSet.of(
                        PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_WRITE,
                        PosixFilePermission.OWNER_EXECUTE,
                        PosixFilePermission.GROUP_READ,
                        PosixFilePermission.GROUP_WRITE,
                        PosixFilePermission.GROUP_EXECUTE));
                return dir;
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("autonomy.scheduled.store-dir", DIR.toString());
        }
    }
}

@QuarkusTest
@QuarkusTestResource(MappingsTestResource.class)
@TestProfile(OtherWritableStoreFileStartupTest.Profile.class)
class OtherWritableStoreFileStartupTest extends StoreFailureStartupTest {

    @AfterAll
    static void cleanup() throws IOException {
        StoreFailureStartupTest.relax(Profile.DIR);
    }

    public static class Profile implements QuarkusTestProfile {
        static final Path DIR = directory();

        private static Path directory() {
            try {
                Path dir = StoreFailureStartupTest.freshDirectory("autonomy-scheduled-other");
                Path file = dir.resolve("note");
                Files.writeString(file, "other-writable");
                Files.setPosixFilePermissions(file, EnumSet.of(
                        PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_WRITE,
                        PosixFilePermission.OTHERS_WRITE));
                return dir;
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("autonomy.scheduled.store-dir", DIR.toString());
        }
    }
}

@QuarkusTest
@QuarkusTestResource(MappingsTestResource.class)
@TestProfile(SymlinkStoreStartupTest.Profile.class)
class SymlinkStoreStartupTest extends StoreFailureStartupTest {

    @AfterAll
    static void cleanup() throws IOException {
        StoreFailureStartupTest.relax(Profile.LINK);
        StoreFailureStartupTest.relax(Profile.REAL);
    }

    public static class Profile implements QuarkusTestProfile {
        static final Path REAL = directory();
        static final Path LINK = link();

        private static Path directory() {
            try {
                return StoreFailureStartupTest.freshDirectory("autonomy-scheduled-real");
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        private static Path link() {
            try {
                Path link = Path.of(System.getProperty("java.io.tmpdir"),
                        "autonomy-scheduled-link-" + UUID.randomUUID());
                Files.createSymbolicLink(link, REAL);
                return link;
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("autonomy.scheduled.store-dir", LINK.toString());
        }
    }
}

@QuarkusTest
@QuarkusTestResource(MappingsTestResource.class)
@TestProfile(BrokenMigrationStartupTest.Profile.class)
class BrokenMigrationStartupTest extends StoreFailureStartupTest {

    @AfterAll
    static void cleanup() throws IOException {
        StoreFailureStartupTest.relax(Profile.DIR);
    }

    public static class Profile implements QuarkusTestProfile {
        static final Path DIR = directory();

        private static Path directory() {
            try {
                return StoreFailureStartupTest.freshDirectory("autonomy-scheduled-migration");
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "autonomy.scheduled.store-dir", DIR.toString(),
                    "autonomy.scheduled.migrations", "classpath:db/scheduled-broken");
        }
    }
}
