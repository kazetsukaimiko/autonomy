package io.freedriver.autonomy.mqtt.contract;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Topic B: {@code freedriver/v1/home/commands} (retain=false, QoS 1). */
public record ApplianceCommandMessage(
        @NotNull @Min(1) @Max(1) Integer schemaVersion,
        @NotBlank String commandId,
        @NotBlank @Size(max = ApplianceSchemas.NAME_MAX) String name,
        @NotNull Boolean on) {

    public static ApplianceCommandMessage parse(String json) {
        try {
            ApplianceCommandMessage parsed = ApplianceSchemas.STRICT.readValue(json, ApplianceCommandMessage.class);
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
