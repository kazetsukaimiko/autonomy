package io.freedriver.autonomy.mqtt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CommandListResultTest {

    @Test
    void threeResultsCoverTheSink() {
        assertEquals("applied", kind(new CommandListResult.Applied("11111111-1111-1111-1111-111111111111")));
        assertEquals("refused", kind(new CommandListResult.Refused("pending full")));
        assertEquals("dropped", kind(new CommandListResult.Dropped("stale")));
    }

    @Test
    void appliedRequiresAListId() {
        assertThrows(IllegalArgumentException.class, () -> new CommandListResult.Applied(null));
        assertThrows(IllegalArgumentException.class, () -> new CommandListResult.Applied(" "));
    }

    @Test
    void refusedAndDroppedRequireAReason() {
        assertThrows(IllegalArgumentException.class, () -> new CommandListResult.Refused(null));
        assertThrows(IllegalArgumentException.class, () -> new CommandListResult.Refused(""));
        assertThrows(IllegalArgumentException.class, () -> new CommandListResult.Dropped(null));
        assertThrows(IllegalArgumentException.class, () -> new CommandListResult.Dropped(" "));
    }

    private static String kind(CommandListResult result) {
        return switch (result) {
            case CommandListResult.Applied _ -> "applied";
            case CommandListResult.Refused _ -> "refused";
            case CommandListResult.Dropped _ -> "dropped";
        };
    }
}
