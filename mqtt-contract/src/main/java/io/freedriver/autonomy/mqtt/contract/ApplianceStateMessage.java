package io.freedriver.autonomy.mqtt.contract;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Topic A: {@code freedriver/v1/home/appliances} (retain=false, QoS 1). */
public record ApplianceStateMessage(
        @NotNull @Min(1) @Max(1) Integer schemaVersion,
        String appliedCommandId,
        @NotNull @Valid List<Appliance> appliances) {

    public ApplianceStateMessage {
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
