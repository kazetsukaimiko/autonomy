package io.freedriver.autonomy.mqtt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class MqttConfigValidatorTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void disabledConfigPassesWithoutRequiredFields() {
        assertTrue(validator.validate(config(false)).isEmpty());
    }

    @Test
    void enabledConfigRequiresInstanceIdentityAndLogin() {
        Set<ConstraintViolation<AutonomyMqttConfig>> violations = validator.validate(config(true));
        assertTrue(violations.stream().anyMatch(violation -> violation.getPropertyPath().toString().equals("instanceId")));
        assertTrue(violations.stream().anyMatch(violation -> violation.getPropertyPath().toString().equals("instanceName")));
        assertTrue(violations.stream().anyMatch(violation -> violation.getPropertyPath().toString().equals("username")));
        assertTrue(violations.stream().anyMatch(violation -> violation.getPropertyPath().toString().equals("passwordFile")));
    }

    @Test
    void enabledConfigAcceptsACompleteSetting() {
        Stub stub = config(true);
        stub.username = Optional.of("autonomy");
        stub.passwordFile = Optional.of("/run/secrets/autonomy.pass");
        stub.instanceId = Optional.of("550e8400-e29b-41d4-a716-446655440000");
        stub.instanceName = Optional.of("Cabin");
        assertTrue(validator.validate(stub).isEmpty());
    }

    @Test
    void instanceIdMustBeAUuid() {
        Stub stub = config(true);
        stub.username = Optional.of("autonomy");
        stub.passwordFile = Optional.of("/run/secrets/autonomy.pass");
        stub.instanceId = Optional.of("not-a-uuid");
        stub.instanceName = Optional.of("Cabin");
        Set<ConstraintViolation<AutonomyMqttConfig>> violations = validator.validate(stub);
        assertEquals(1, violations.size());
        assertEquals("instanceId", violations.iterator().next().getPropertyPath().toString());
    }

    private static Stub config(boolean enabled) {
        Stub stub = new Stub();
        stub.enabled = enabled;
        return stub;
    }

    static Stub enabledStub() {
        return config(true);
    }

    static final class Stub implements AutonomyMqttConfig {
        private boolean enabled;
        String host = "mqtt.freedriver.io";
        private int port = 8883;
        private Optional<String> username = Optional.empty();
        private Optional<String> passwordFile = Optional.empty();
        private Optional<String> caFile = Optional.empty();
        private Optional<String> clientId = Optional.empty();
        private Optional<String> instanceId = Optional.empty();
        private Optional<String> instanceName = Optional.empty();
        private Duration publishInterval = Duration.ofSeconds(10);
        private Duration keepalive = Duration.ofSeconds(60);
        private Duration connectTimeout = Duration.ofSeconds(10);

        @Override
        public boolean enabled() {
            return enabled;
        }

        @Override
        public String host() {
            return host;
        }

        @Override
        public int port() {
            return port;
        }

        @Override
        public Optional<String> username() {
            return username;
        }

        @Override
        public Optional<String> passwordFile() {
            return passwordFile;
        }

        @Override
        public Optional<String> caFile() {
            return caFile;
        }

        @Override
        public Optional<String> clientId() {
            return clientId;
        }

        @Override
        public Optional<String> instanceId() {
            return instanceId;
        }

        @Override
        public Optional<String> instanceName() {
            return instanceName;
        }

        @Override
        public Duration publishInterval() {
            return publishInterval;
        }

        @Override
        public Duration keepalive() {
            return keepalive;
        }

        @Override
        public Duration connectTimeout() {
            return connectTimeout;
        }
    }
}
