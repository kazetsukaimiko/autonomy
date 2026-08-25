package io.freedriver.autonomy.mqtt.contract;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Topic A: {@code freedriver/v1/{instanceId}/appliances} (retain=false, QoS 1).
 * Isolation is the autonomy instance. Boards are not on this wire.
 * {@code instanceId} is UUIDv4, not the MQTT protocol client-id.
 * {@code instanceName} is UX-only and is never a topic segment or ACL.
 */
public record ApplianceStateMessage(
        @NotNull @Min(ApplianceSchemas.SCHEMA_VERSION) @Max(ApplianceSchemas.SCHEMA_VERSION)
                Integer schemaVersion,
        @NotNull UUID instanceId,
        @NotBlank String instanceName,
        String appliedCommandId,
        @NotNull @Valid List<Appliance> appliances) {

    public ApplianceStateMessage {
        ApplianceSchemas.requireInstanceId(instanceId);
        if (appliances != null) {
            appliances = List.copyOf(appliances);
        }
    }

    public static ApplianceStateMessage parse(String json) {
        try {
            ApplianceStateMessage parsed = ApplianceSchemas.STRICT.readValue(json, ApplianceStateMessage.class);
            return ApplianceSchemas.validate(parsed);
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
