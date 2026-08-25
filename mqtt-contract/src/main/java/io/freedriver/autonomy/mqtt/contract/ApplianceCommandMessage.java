package io.freedriver.autonomy.mqtt.contract;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.NonNull;

/**
 * Topic B: {@code freedriver/v1/{instanceId}/commands} (retain=false, QoS 1).
 * Isolation is the autonomy instance. Boards are not on this wire.
 * {@code instanceId} is a UUID, not the MQTT protocol client-id.
 */
@Builder(toBuilder = true)
public record ApplianceCommandMessage(
        @NonNull @NotNull UUID instanceId,
        @NonNull @NotBlank String commandId,
        @NonNull @NotBlank @Size(max = ApplianceSchemas.NAME_MAX) String applianceName,
        @NonNull @NotNull Boolean on) {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    public static ApplianceCommandMessage parse(String json) {
        try {
            ApplianceCommandMessage parsed =
                    ApplianceSchemas.STRICT.readValue(json, ApplianceCommandMessage.class);
            Set<ConstraintViolation<ApplianceCommandMessage>> violations = VALIDATOR.validate(parsed);
            if (!violations.isEmpty()) {
                String detail = violations.stream()
                        .map(v -> v.getPropertyPath() + " " + v.getMessage())
                        .collect(Collectors.joining("; "));
                throw new IllegalArgumentException("Rejected MQTT contract: " + detail);
            }
            return parsed;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Rejected appliance command: " + e.getMessage(), e);
        }
    }

    public String toJson() {
        try {
            return ApplianceSchemas.STRICT.writeValueAsString(this);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize appliance command", e);
        }
    }
}
