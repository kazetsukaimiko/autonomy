package io.freedriver.autonomy.mqtt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.jboss.logmanager.ExtHandler;
import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.Level;
import org.jboss.logmanager.Logger;
import org.junit.jupiter.api.Test;

class MqttStatePublisherTest {

    private static final String SECRET = "xK9mQ2vL7p";

    @Test
    void invalidEnabledConfigLogsTheKeysAndLeavesMqttOff() {
        Logger logger = Logger.getLogger(MqttLog.CATEGORY);
        java.util.logging.Level previous = logger.getLevel();
        Capture capture = new Capture();
        logger.setLevel(Level.ALL);
        logger.addHandler(capture);
        try {
            MqttConfigValidatorTest.Stub stub = MqttConfigValidatorTest.enabledStub();
            stub.host = SECRET + "://broker";
            Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
            MqttStatePublisher publisher = new MqttStatePublisher(stub, validator, () -> {
                throw new AssertionError("state source read");
            });
            publisher.onStart(null);
            List<ExtLogRecord> errors = capture.records.stream()
                    .filter(record -> record.getLevel().equals(Level.ERROR))
                    .toList();
            assertEquals(1, errors.size());
            String message = errors.get(0).getFormattedMessage();
            assertTrue(message.startsWith("MQTT off; invalid configuration keys="));
            assertTrue(message.contains("autonomy.mqtt.host"));
            assertTrue(message.contains("autonomy.mqtt.instanceId"));
            assertTrue(message.contains("autonomy.mqtt.passwordFile"));
            assertFalse(message.contains(SECRET));
            assertTrue(capture.records.stream().noneMatch(record ->
                    record.getFormattedMessage().contains("connecting host=")));
        } finally {
            logger.removeHandler(capture);
            logger.setLevel(previous);
        }
    }

    @Test
    void invalidKeyListNamesKeysAndOmitsValues() {
        Set<jakarta.validation.ConstraintViolation<MqttStartupConfig>> violations =
                Validation.buildDefaultValidatorFactory().getValidator()
                        .validate(new MqttStartupConfig(MqttConfigValidatorTest.enabledStub()));
        String keys = MqttStatePublisher.invalidKeys(violations);
        assertTrue(keys.contains("autonomy.mqtt.instanceId"));
        assertTrue(keys.contains("autonomy.mqtt.passwordFile"));
        assertFalse(keys.contains("is required"));
        assertFalse(keys.contains(SECRET));
    }

    private static final class Capture extends ExtHandler {
        private final List<ExtLogRecord> records = new ArrayList<>();

        @Override
        public void publish(java.util.logging.LogRecord record) {
            records.add(ExtLogRecord.wrap(record));
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }
}
