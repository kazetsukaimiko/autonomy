package io.freedriver.autonomy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

/**
 * Points {@code autonomy.mappings.file} at a temporary mapping before the application starts.
 */
public class ProxyBoardTestResource implements QuarkusTestResourceLifecycleManager {

    private static final String MAPPING = """
            {"eventTTL":7,"eventTTLUnit":"DAYS","mappings":[{
              "connectorId":"11111111-1111-4111-8111-111111111111",
              "connectorName":"main",
              "appliances":[
                {"identifier":40,"name":"fridge"},
                {"identifier":41,"name":"hallway"},
                {"identifier":42,"name":"oven"}
              ],
              "controlMap":{},
              "analogSensors":[],
              "analogAlerts":[]
            }]}
            """;

    private Path mappings;

    @Override
    public Map<String, String> start() {
        try {
            mappings = Files.createTempFile("mappings_v2", ".json");
            Files.writeString(mappings, MAPPING);
        } catch (IOException e) {
            throw new IllegalStateException("Could not prepare mappings file", e);
        }
        return Map.of("autonomy.mappings.file", mappings.toString());
    }

    @Override
    public void stop() {
        try {
            if (mappings != null) {
                Files.deleteIfExists(mappings);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not delete " + mappings, e);
        }
    }
}
