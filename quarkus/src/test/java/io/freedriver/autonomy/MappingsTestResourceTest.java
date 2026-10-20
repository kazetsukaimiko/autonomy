package io.freedriver.autonomy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;

class MappingsTestResourceTest {

    @Test
    void writesTheFixtureOnlyToATemporaryMappingsFile() throws Exception {
        Path defaults = TestMappings.defaultMappingsFile().toAbsolutePath().normalize();
        String before = TestMappings.fingerprint(defaults);

        MappingsTestResource resource = new MappingsTestResource();
        Map<String, String> config = resource.start();
        Path written = Path.of(config.get(TestMappings.CONFIG_KEY)).toAbsolutePath().normalize();
        try {
            assertEquals(1, config.size());
            assertEquals(TestMappings.CONFIG_KEY, config.keySet().iterator().next());
            assertNotEquals(defaults, written);
            assertTrue(Files.isRegularFile(written));
            assertEquals(TestMappings.EMPTY_MAPPINGS, Files.readString(written));
            assertTrue(written.startsWith(
                    Path.of(System.getProperty("java.io.tmpdir")).toAbsolutePath().normalize()));
            assertEquals(before, TestMappings.fingerprint(defaults));
            resource.stop();
            assertEquals(before, TestMappings.fingerprint(defaults));
        } finally {
            deleteFixture(written);
        }
    }

    private static void deleteFixture(Path written) throws IOException {
        Files.deleteIfExists(written);
        Path parent = written.getParent();
        if (parent != null) {
            Files.deleteIfExists(parent);
        }
    }
}
