package io.freedriver.autonomy;

import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

/**
 * Points {@code autonomy.mappings.file} at a temporary mappings file before the application starts.
 * The fixture stays until the JVM exits so shutdown can still read it.
 */
public class MappingsTestResource implements QuarkusTestResourceLifecycleManager {

    @Override
    public Map<String, String> start() {
        return Map.of(TestMappings.CONFIG_KEY, TestMappings.writeTemporary().toString());
    }

    @Override
    public void stop() {
        TestMappings.assertDefaultUntouched();
    }
}
