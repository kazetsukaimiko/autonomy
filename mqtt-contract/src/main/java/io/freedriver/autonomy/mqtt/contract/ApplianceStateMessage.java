package io.freedriver.autonomy.mqtt.contract;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.NonNull;

/**
 * Topic A: {@code freedriver/v1/{instanceId}/appliances} (retain=false, QoS 1).
 * Isolation is the autonomy instance. Boards are not on this wire.
 * {@code instanceId} is UUIDv4, not the MQTT protocol client-id.
 * {@code instanceName} is UX-only and is never a topic segment or ACL.
 */
@Builder(toBuilder = true)
public record ApplianceStateMessage(
        @NonNull @NotNull @UuidV4 UUID instanceId,
        @NonNull @NotBlank String instanceName,
        String appliedCommandId,
        @NonNull @NotNull @Valid List<Appliance> appliances) {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    public ApplianceStateMessage {
        appliances = List.copyOf(appliances);
    }

    public static ApplianceStateMessage parse(String json) {
        try {
            ApplianceStateMessage parsed = ApplianceSchemas.STRICT.readValue(json, ApplianceStateMessage.class);
            Set<ConstraintViolation<ApplianceStateMessage>> violations = VALIDATOR.validate(parsed);
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
            throw new IllegalArgumentException("Rejected appliance state: " + e.getMessage(), e);
        }
    }

    public String toJson() {
        try {
            return ApplianceSchemas.STRICT.writeValueAsString(this);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize appliance state", e);
        }
    }
}
