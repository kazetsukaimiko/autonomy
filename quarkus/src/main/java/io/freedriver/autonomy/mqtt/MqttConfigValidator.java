package io.freedriver.autonomy.mqtt;

import java.util.UUID;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Startup rules for {@code autonomy.mqtt.*}. Typed text is parsed here.
 * Required identity keys apply when {@code enabled} is true.
 */
public class MqttConfigValidator implements ConstraintValidator<MqttConfigValid, MqttStartupConfig> {

    @Override
    public boolean isValid(MqttStartupConfig value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        boolean valid = true;
        Boolean enabled = MqttConfigValues.enabled(value.enabled());
        if (enabled == null) {
            valid = violation(context, "enabled", "must be true or false");
        }
        if (MqttConfigValues.port(value.port()) == null) {
            valid &= violation(context, "port", "must be 1-65535");
        }
        valid &= requireDuration(context, "publishInterval", value.publishInterval());
        valid &= requireDuration(context, "keepalive", value.keepalive());
        valid &= requireDuration(context, "connectTimeout", value.connectTimeout());
        if (!Boolean.TRUE.equals(enabled)) {
            return valid;
        }
        valid &= requireText(context, "host", value.host(), "is required");
        if (value.host() != null && (value.host().contains("://") || value.host().contains("/"))) {
            valid &= violation(context, "host", "must be a hostname or IPv4 address");
        }
        valid &= requirePresent(context, "username", value.username(), "is required");
        valid &= requirePresent(context, "passwordFile", value.passwordFile(), "is required");
        valid &= requireUuid(context, value.instanceId());
        valid &= requirePresent(context, "instanceName", value.instanceName(), "is required");
        if (value.clientId().isPresent() && value.clientId().get().isBlank()) {
            valid &= violation(context, "clientId", "must not be blank");
        }
        if (value.caFile().isPresent() && value.caFile().get().isBlank()) {
            valid &= violation(context, "caFile", "must not be blank");
        }
        return valid;
    }

    private static boolean requireDuration(ConstraintValidatorContext context, String node, String raw) {
        if (MqttConfigValues.duration(raw) == null) {
            return violation(context, node, "must be positive");
        }
        return true;
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

    private static boolean violation(ConstraintValidatorContext context, String node, String message) {
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(node)
                .addConstraintViolation();
        return false;
    }
}
