package io.freedriver.autonomy.mqtt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CommandSubscriptionTest {

    @Test
    void onlyTrueSubscribesWhenMqttIsOnAndTheLimitParses() {
        CommandSubscription on = CommandSubscription.decide("true", "4096", true);
        assertTrue(on.subscribe());
        assertEquals(4096, on.maxBytes());
        assertNull(on.reason());

        CommandSubscription trimmed = CommandSubscription.decide(" true ", " 512 ", true);
        assertTrue(trimmed.subscribe());
        assertEquals(512, trimmed.maxBytes());
    }

    @Test
    void anyOtherValueStaysUnsubscribedWithoutTheRawValue() {
        String[] values = {null, "", "false", "FALSE", "TRUE", "yes", "1", "banana", " true true "};
        for (String value : values) {
            CommandSubscription decision = CommandSubscription.decide(value, "4096", true);
            assertFalse(decision.subscribe(), String.valueOf(value));
            assertEquals("autonomy.mqtt.commands-enabled is not true", decision.reason());
            assertFalse(decision.reason().contains("banana"));
        }
    }

    @Test
    void trueDoesNotSubscribeWhenMqttIsOff() {
        CommandSubscription decision = CommandSubscription.decide("true", "4096", false);
        assertFalse(decision.subscribe());
        assertEquals("autonomy.mqtt.enabled is not true", decision.reason());
    }

    @Test
    void aBadLimitDoesNotSubscribeAndDoesNotKeepTheRawValue() {
        for (String limit : new String[] {null, "", "0", "-1", "abc", "4096abc"}) {
            CommandSubscription decision = CommandSubscription.decide("true", limit, true);
            assertFalse(decision.subscribe(), limit);
            assertEquals("autonomy.mqtt.command-max-bytes is invalid", decision.reason());
            assertFalse(decision.reason().contains("abc"));
        }
    }
}
