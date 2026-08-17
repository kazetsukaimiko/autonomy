package io.freedriver.autonomy.entity.view;

import io.freedriver.victron.VictronDevice;
import lombok.Builder;

@Builder(toBuilder = true)
public record ControllerView(
        VictronDevice device,
        ControllerTimeView controllerTime,
        ControllerStateView controllerState,
        ControllerHistoryView controllerHistory) {
}
