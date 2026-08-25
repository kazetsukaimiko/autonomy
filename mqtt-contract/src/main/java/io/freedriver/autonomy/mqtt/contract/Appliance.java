package io.freedriver.autonomy.mqtt.contract;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.NonNull;

@Builder(toBuilder = true)
public record Appliance(
        @NonNull @NotBlank @Size(max = ApplianceSchemas.NAME_MAX) String applianceName,
        @NonNull @NotNull Boolean on) {}
