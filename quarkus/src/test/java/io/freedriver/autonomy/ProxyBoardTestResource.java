package io.freedriver.autonomy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

/**
 * Installs a one-board mapping before the application starts, and puts the previous file back after.
 */
public class ProxyBoardTestResource implements QuarkusTestResourceLifecycleManager {

    private static final Path MAPPINGS = Path.of(
            System.getProperty("user.home"), ".config", "autonomy", "mappings_v2.json");

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

    private byte[] previous;
    private boolean hadFile;

    @Override
    public Map<String, String> start() {
        try {
            Files.createDirectories(MAPPINGS.getParent());
            if (Files.exists(MAPPINGS)) {
                hadFile = true;
                previous = Files.readAllBytes(MAPPINGS);
            }
            Files.writeString(MAPPINGS, MAPPING);
        } catch (IOException e) {
            throw new IllegalStateException("Could not prepare " + MAPPINGS, e);
        }
        return Map.of();
    }

    @Override
    public void stop() {
        try {
            if (hadFile) {
                Files.write(MAPPINGS, previous);
            } else {
                Files.deleteIfExists(MAPPINGS);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not restore " + MAPPINGS, e);
        }
    }
}
