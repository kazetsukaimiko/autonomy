package io.freedriver.autonomy.mqtt;

import java.time.Duration;
import java.util.UUID;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Startup rules for {@code autonomy.mqtt.*} when publishing is turned on.
 */
public class MqttConfigValidator implements ConstraintValidator<MqttConfigValid, AutonomyMqttConfig> {

    @Override
    public boolean isValid(AutonomyMqttConfig value, ConstraintValidatorContext context) {
        if (value == null || !value.enabled()) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        boolean valid = true;
        valid &= requireText(context, "host", value.host(), "is required");
        if (value.host() != null && (value.host().contains("://") || value.host().contains("/"))) {
            valid &= violation(context, "host", "must be a hostname or IPv4 address");
        }
        if (value.port() < 1 || value.port() > 65535) {
            valid &= violation(context, "port", "must be 1-65535");
        }
        valid &= requirePresent(context, "username", value.username(), "is required");
        valid &= requirePresent(context, "passwordFile", value.passwordFile(), "is required");
        valid &= requireUuid(context, value.instanceId());
        valid &= requirePresent(context, "instanceName", value.instanceName(), "is required");
        valid &= requirePositive(context, "publishInterval", value.publishInterval());
        valid &= requirePositive(context, "keepalive", value.keepalive());
        valid &= requirePositive(context, "connectTimeout", value.connectTimeout());
        if (value.clientId().isPresent() && value.clientId().get().isBlank()) {
            valid &= violation(context, "clientId", "must not be blank");
        }
        if (value.caFile().isPresent() && value.caFile().get().isBlank()) {
            valid &= violation(context, "caFile", "must not be blank");
        }
        return valid;
    }

    private static boolean requireText(ConstraintValidatorContext context, String node, String text, String message) {
        if (text == null || text.isBlank()) {
            return violation(context, node, message);
        }
        return true;
    }

    private static boolean requirePresent(ConstraintValidatorContext context, String node,
            java.util.Optional<String> value, String message) {
        if (value == null || value.map(String::isBlank).orElse(true)) {
            return violation(context, node, message);
        }
        return true;
    }

    private static boolean requireUuid(ConstraintValidatorContext context, java.util.Optional<String> value) {
        if (value == null || value.map(String::isBlank).orElse(true)) {
            return violation(context, "instanceId", "is required");
        }
        try {
            UUID.fromString(value.get());
            return true;
        } catch (IllegalArgumentException e) {
            return violation(context, "instanceId", "must be a UUID");
        }
    }

    private static boolean requirePositive(ConstraintValidatorContext context, String node, Duration duration) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            return violation(context, node, "must be positive");
        }
        return true;
    }

    private static boolean violation(ConstraintValidatorContext context, String node, String message) {
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(node)
                .addConstraintViolation();
        return false;
    }
}
