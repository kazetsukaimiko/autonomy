package io.freedriver.autonomy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Ensures {@code ~/.config/autonomy/mappings_v2.json} exists so analog refresh can read configuration during tests.
 */
public final class TestMappings {

    private static final Path MAPPINGS = Path.of(
            System.getProperty("user.home"), ".config", "autonomy", "mappings_v2.json");
    private static final String EMPTY_MAPPINGS = """
            {"eventTTL":7,"eventTTLUnit":"DAYS","mappings":[]}
            """;

    private TestMappings() {
    }

    public static void ensure() {
        try {
            if (Files.exists(MAPPINGS)) {
                return;
            }
            Files.createDirectories(MAPPINGS.getParent());
            Files.writeString(MAPPINGS, EMPTY_MAPPINGS);
        } catch (IOException e) {
            throw new IllegalStateException("Could not prepare " + MAPPINGS, e);
        }
    }
}
