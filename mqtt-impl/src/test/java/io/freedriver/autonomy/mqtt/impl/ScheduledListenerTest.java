package io.freedriver.autonomy.mqtt.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class ScheduledListenerTest {

    @Test
    void firedRejectsANullEntryId() {
        PahoMqttConnector connector = new PahoMqttConnector();
        assertThrows(IllegalArgumentException.class, () -> connector.scheduledListener().fired(null));
    }

    @Test
    void firedBeforeStartAcceptsAnEntryId() {
        PahoMqttConnector connector = new PahoMqttConnector();
        connector.scheduledListener().fired(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    }
}
