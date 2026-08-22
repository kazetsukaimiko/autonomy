package io.freedriver.autonomy.event.input.joystick.jstest;

import java.util.Map;

import lombok.Builder;
import lombok.NonNull;

@Builder(toBuilder = true)
public record JSMetadata(
        String title,
        String hardwareType,
        String driverVersion,
        @NonNull Map<Integer, String> axisNames,
        @NonNull Map<Integer, String> buttonNames) {

    public JSMetadata {
        axisNames = axisNames == null ? Map.of() : Map.copyOf(axisNames);
        buttonNames = buttonNames == null ? Map.of() : Map.copyOf(buttonNames);
    }

    public static JSMetadata empty() {
        return new JSMetadata(null, null, null, Map.of(), Map.of());
    }
}
