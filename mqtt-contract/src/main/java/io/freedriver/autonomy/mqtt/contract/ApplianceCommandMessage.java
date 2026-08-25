package io.freedriver.autonomy.mqtt.contract;

import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Topic B: {@code freedriver/v1/{instanceId}/commands} (retain=false, QoS 1).
 * Isolation is the autonomy instance. Boards are not on this wire.
 * {@code instanceId} is UUIDv4, not the MQTT protocol client-id.
 */
public record ApplianceCommandMessage(
        @NotNull @Min(ApplianceSchemas.SCHEMA_VERSION) @Max(ApplianceSchemas.SCHEMA_VERSION)
                Integer schemaVersion,
        @NotNull UUID instanceId,
        @NotBlank String commandId,
        @NotBlank @Size(max = ApplianceSchemas.NAME_MAX) String applianceName,
        @NotNull Boolean on) {

    public ApplianceCommandMessage {
        ApplianceSchemas.requireInstanceId(instanceId);
    }

    public static ApplianceCommandMessage parse(String json) {
        try {
            ApplianceCommandMessage parsed =
                    ApplianceSchemas.STRICT.readValue(json, ApplianceCommandMessage.class);
            return ApplianceSchemas.validate(parsed);
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
