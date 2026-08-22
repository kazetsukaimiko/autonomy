package io.freedriver.autonomy.mqtt.contract;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record Appliance(String name, boolean on) {

    @JsonCreator
    public Appliance(
            @JsonProperty(value = "name", required = true) String name,
            @JsonProperty(value = "on", required = true) boolean on) {
        if (!ApplianceSchemas.validName(name)) {
            throw new IllegalArgumentException("invalid appliance name");
        }
        this.name = name;
        this.on = on;
    }
}

