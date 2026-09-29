package io.freedriver.autonomy.mqtt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertTrue(validator.validate(new MqttStartupConfig(config("false"))).isEmpty());
    }

    @Test
    void enabledConfigRequiresInstanceIdentityAndLogin() {
        Set<ConstraintViolation<MqttStartupConfig>> violations = validator.validate(new MqttStartupConfig(config("true")));
        assertTrue(violations.stream().anyMatch(violation -> violation.getPropertyPath().toString().equals("instanceId")));
        assertTrue(violations.stream().anyMatch(violation -> violation.getPropertyPath().toString().equals("instanceName")));
        assertTrue(violations.stream().anyMatch(violation -> violation.getPropertyPath().toString().equals("username")));
        assertTrue(violations.stream().anyMatch(violation -> violation.getPropertyPath().toString().equals("passwordFile")));
    }

    @Test
    void enabledConfigAcceptsACompleteSetting() {
        assertTrue(validator.validate(new MqttStartupConfig(complete())).isEmpty());
    }

    @Test
    void instanceIdMustBeAUuid() {
        Stub stub = complete();
        stub.instanceId = Optional.of("not-a-uuid");
        assertOnly(stub, "instanceId");
    }

    @Test
    void enabledRejectsABadValueAndAcceptsTrueAndFalse() {
        Stub bad = complete();
        bad.enabled = "maybe";
        assertOnly(bad, "enabled");
        Stub outOfRange = complete();
        outOfRange.enabled = "2";
        assertOnly(outOfRange, "enabled");
        Stub on = complete();
        on.enabled = "TRUE";
        assertTrue(validator.validate(new MqttStartupConfig(on)).isEmpty());
        Stub off = complete();
        off.enabled = "false";
        assertTrue(validator.validate(new MqttStartupConfig(off)).isEmpty());
    }

    @Test
    void portRejectsABadValueAnOutOfRangeValueAndAcceptsAGoodValue() {
        Stub bad = complete();
        bad.port = "abc";
        assertOnly(bad, "port");
        Stub low = complete();
        low.port = "0";
        assertOnly(low, "port");
        Stub high = complete();
        high.port = "65536";
        assertOnly(high, "port");
        Stub good = complete();
        good.port = "8883";
        assertTrue(validator.validate(new MqttStartupConfig(good)).isEmpty());
        Stub edge = complete();
        edge.port = "1";
        assertTrue(validator.validate(new MqttStartupConfig(edge)).isEmpty());
        Stub top = complete();
        top.port = "65535";
        assertTrue(validator.validate(new MqttStartupConfig(top)).isEmpty());
    }

    @Test
    void publishIntervalRejectsABadValueAnOutOfRangeValueAndAcceptsAGoodValue() {
        assertDuration("publishInterval", "soon", "0s", "10s");
    }

    @Test
    void keepaliveRejectsABadValueAnOutOfRangeValueAndAcceptsAGoodValue() {
        assertDuration("keepalive", "soon", "-1s", "60s");
    }

    @Test
    void connectTimeoutRejectsABadValueAnOutOfRangeValueAndAcceptsAGoodValue() {
        assertDuration("connectTimeout", "soon", "0s", "10s");
    }

    private void assertDuration(String key, String bad, String outOfRange, String good) {
        Stub malformed = complete();
        setDuration(malformed, key, bad);
        assertOnly(malformed, key);
        Stub range = complete();
        setDuration(range, key, outOfRange);
        assertOnly(range, key);
        Stub accepted = complete();
        setDuration(accepted, key, good);
        assertTrue(validator.validate(new MqttStartupConfig(accepted)).isEmpty());
    }

    private static void setDuration(Stub stub, String key, String value) {
        switch (key) {
            case "publishInterval" -> stub.publishInterval = value;
            case "keepalive" -> stub.keepalive = value;
            case "connectTimeout" -> stub.connectTimeout = value;
            default -> throw new IllegalArgumentException(key);
        }
    }

    private void assertOnly(Stub stub, String key) {
        Set<ConstraintViolation<MqttStartupConfig>> violations = validator.validate(new MqttStartupConfig(stub));
        assertEquals(1, violations.size());
        assertEquals(key, violations.iterator().next().getPropertyPath().toString());
    }

    private static Stub config(String enabled) {
        Stub stub = new Stub();
        stub.enabled = enabled;
        return stub;
    }

    private static Stub complete() {
        Stub stub = config("true");
        stub.username = Optional.of("autonomy");
        stub.passwordFile = Optional.of("/run/secrets/autonomy.pass");
        stub.instanceId = Optional.of("550e8400-e29b-41d4-a716-446655440000");
        stub.instanceName = Optional.of("Cabin");
        return stub;
    }

    static Stub enabledStub() {
        return config("true");
    }

    static final class Stub implements AutonomyMqttConfig {
        private String enabled = "false";
        String host = "mqtt.freedriver.io";
        String port = "8883";
        private Optional<String> username = Optional.empty();
        private Optional<String> passwordFile = Optional.empty();
        private Optional<String> caFile = Optional.empty();
        private Optional<String> clientId = Optional.empty();
        private Optional<String> instanceId = Optional.empty();
        private Optional<String> instanceName = Optional.empty();
        private String publishInterval = "10s";
        private String keepalive = "60s";
        private String connectTimeout = "10s";

        @Override
        public String enabled() {
            return enabled;
        }

        @Override
        public String host() {
            return host;
        }

        @Override
        public String port() {
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
        public String publishInterval() {
            return publishInterval;
        }

        @Override
        public String keepalive() {
            return keepalive;
        }

        @Override
        public String connectTimeout() {
            return connectTimeout;
        }
    }
}
