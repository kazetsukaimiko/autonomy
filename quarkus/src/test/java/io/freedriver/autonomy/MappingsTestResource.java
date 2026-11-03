package io.freedriver.autonomy;

import java.nio.file.Path;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

/**
 * Points {@code autonomy.mappings.file} at a temporary mappings file before the application starts.
 * The fixture stays until the JVM exits so shutdown can still read it.
 */
public class MappingsTestResource implements QuarkusTestResourceLifecycleManager {

    @Override
    public Map<String, String> start() {
        Path mappings = TestMappings.writeTemporary();
        Path connections = mappings.getParent().resolve("connections.d");
        return Map.of(
                TestMappings.CONFIG_KEY, mappings.toString(),
                TestMappings.CONNECTIONS_DIR_KEY, connections.toString());
    }

    @Override
    public void stop() {
        TestMappings.assertDefaultUntouched();
    }
}
