package io.freedriver.autonomy.mqtt;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class MqttConnectorCommandListTest {

    @Test
    void defaultListenerRejectsANullEntryId() {
        MqttConnector connector = new StubConnector();
        assertThrows(IllegalArgumentException.class, () -> connector.scheduledListener().fired(null));
    }

    @Test
    void defaultListenerAcceptsAnEntryId() {
        MqttConnector connector = new StubConnector();
        connector.scheduledListener().fired(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    }

    private static final class StubConnector implements MqttConnector {

        @Override
        public void start(MqttSettings settings, ApplianceStateSource states) {
        }

        @Override
        public void start(MqttSettings settings, ApplianceStateSource states, ApplianceCommandSink commands) {
        }

        @Override
        public void publishExact(String topic, byte[] payload, int qos, boolean retain) {
        }

        @Override
        public void close() {
        }
    }
}
