package io.freedriver.autonomy.mqtt.contract;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record Appliance(
        @NotBlank @Size(max = ApplianceSchemas.NAME_MAX) String name, @NotNull Boolean on) {}
