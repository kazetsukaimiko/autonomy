package io.freedriver.autonomy.entity.view;

import kaze.victron.VictronDevice;

public class ControllerView {
    private final VictronDevice device;

    private final ControllerTimeView controllerTime;
    private final ControllerStateView controllerState;

    public ControllerView(VictronDevice device, ControllerTimeView controllerTime, ControllerStateView controllerState) {
        this.device = device;
        this.controllerTime = controllerTime;
        this.controllerState = controllerState;
    }

    public VictronDevice getDevice() {
        return device;
    }

    public ControllerTimeView getControllerTime() {
        return controllerTime;
    }
}
