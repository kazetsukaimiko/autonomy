package io.freedriver.autonomy.mqtt.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class ReconnectPolicyTest {

    @Test
    void growsUntilTheCapAndKeepsJitterInsideTheFraction() {
        ReconnectPolicy steady = new ReconnectPolicy(Duration.ofSeconds(1), Duration.ofSeconds(60), 0.2, () -> 0);
        assertEquals(Duration.ofSeconds(1), steady.delay(1));
        assertEquals(Duration.ofSeconds(2), steady.delay(2));
        assertEquals(Duration.ofSeconds(32), steady.delay(6));
        assertEquals(Duration.ofSeconds(60), steady.delay(7));
        assertEquals(Duration.ofSeconds(60), steady.delay(40));

        ReconnectPolicy jittered = new ReconnectPolicy(Duration.ofSeconds(1), Duration.ofSeconds(60), 0.2, () -> 1);
        assertEquals(Duration.ofMillis(800), jittered.delay(1));
        assertEquals(Duration.ofSeconds(48), jittered.delay(40));
        assertTrue(jittered.delay(3).compareTo(Duration.ofSeconds(4)) < 0);
    }
}
