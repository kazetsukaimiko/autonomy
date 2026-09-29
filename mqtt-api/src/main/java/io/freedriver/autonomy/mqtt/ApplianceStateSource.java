package io.freedriver.autonomy.mqtt;

import java.io.IOException;
import java.util.Map;

/**
 * Appliance on/off the publisher reads. Implementations stay off the serial boards.
 */
public interface ApplianceStateSource {

    Snapshot read() throws IOException;

    /**
     * @param states      name to on/off for pins the cache already knows
     * @param mappedCount deduped names from the mappings, cached or not
     */
    record Snapshot(Map<String, Boolean> states, int mappedCount) {

        public Snapshot {
            states = states == null ? Map.of() : Map.copyOf(states);
            if (mappedCount < 0) {
                throw new IllegalArgumentException("mappedCount");
            }
        }
    }
}
