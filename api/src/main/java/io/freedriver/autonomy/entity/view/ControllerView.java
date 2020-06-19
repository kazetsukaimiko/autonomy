package io.freedriver.autonomy.entity.view;

import io.freedriver.victron.VictronDevice;

public class ControllerView {
    private final VictronDevice device;

    private final ControllerTimeView controllerTime;
    private final ControllerStateView controllerState;
    private final ControllerHistoryView controllerHistory;

    public ControllerView(VictronDevice device, ControllerTimeView controllerTime, ControllerStateView controllerState, ControllerHistoryView controllerHistory) {
        this.device = device;
        this.controllerTime = controllerTime;
        this.controllerState = controllerState;
        this.controllerHistory = controllerHistory;
    }

    public VictronDevice getDevice() {
        return device;
    }

    public ControllerTimeView getControllerTime() {
        return controllerTime;
    }

    public ControllerStateView getControllerState() {
        return controllerState;
    }

    public ControllerHistoryView getControllerHistory() {
        return controllerHistory;
    }
}
