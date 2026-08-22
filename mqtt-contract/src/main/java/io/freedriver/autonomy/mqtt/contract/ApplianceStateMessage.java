package io.freedriver.autonomy.mqtt.contract;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Topic A: {@code freedriver/v1/home/appliances} (retain=false, QoS 1). */
public record ApplianceStateMessage(int schemaVersion, String appliedCommandId, List<Appliance> appliances) {

    @JsonCreator
    public ApplianceStateMessage(
            @JsonProperty(value = "schemaVersion", required = true) int schemaVersion,
            @JsonProperty("appliedCommandId") String appliedCommandId,
            @JsonProperty(value = "appliances", required = true) List<Appliance> appliances) {
        if (schemaVersion != ApplianceSchemas.SCHEMA_VERSION) {
            throw new IllegalArgumentException("schemaVersion must be 1");
        }
        if (appliances == null) {
            throw new IllegalArgumentException("appliances required");
        }
        this.schemaVersion = schemaVersion;
        this.appliedCommandId = appliedCommandId;
        this.appliances = List.copyOf(appliances);
    }

    public static ApplianceStateMessage parse(String json) {
        try {
            return ApplianceSchemas.STRICT.readValue(json, ApplianceStateMessage.class);
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
