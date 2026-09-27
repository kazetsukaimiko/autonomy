package io.freedriver.autonomy;

import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

/**
 * Prepares {@code mappings_v2.json} before the application starts.
 */
public class MappingsTestResource implements QuarkusTestResourceLifecycleManager {

    @Override
    public Map<String, String> start() {
        TestMappings.ensure();
        return Map.of();
    }

    @Override
    public void stop() {
    }
}
