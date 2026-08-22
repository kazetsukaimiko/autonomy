package io.freedriver.autonomy.mqtt.contract;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Topic B: {@code freedriver/v1/home/commands} (retain=false, QoS 1). */
public record ApplianceCommandMessage(int schemaVersion, String commandId, String name, boolean on) {

    @JsonCreator
    public ApplianceCommandMessage(
            @JsonProperty(value = "schemaVersion", required = true) int schemaVersion,
            @JsonProperty(value = "commandId", required = true) String commandId,
            @JsonProperty(value = "name", required = true) String name,
            @JsonProperty(value = "on", required = true) boolean on) {
        if (schemaVersion != ApplianceSchemas.SCHEMA_VERSION) {
            throw new IllegalArgumentException("schemaVersion must be 1");
        }
        if (commandId == null || commandId.isBlank()) {
            throw new IllegalArgumentException("commandId required");
        }
        if (!ApplianceSchemas.validName(name)) {
            throw new IllegalArgumentException("invalid appliance name");
        }
        this.schemaVersion = schemaVersion;
        this.commandId = commandId;
        this.name = name;
        this.on = on;
    }

    public static ApplianceCommandMessage parse(String json) {
        try {
            return ApplianceSchemas.STRICT.readValue(json, ApplianceCommandMessage.class);
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

