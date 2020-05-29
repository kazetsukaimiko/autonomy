package io.freedriver.autonomy.entity.view;

import kaze.victron.VictronDevice;

public class ControllerView {
    private final VictronDevice device;

    private final ControllerTimeView controllerTime;

    public ControllerView(VictronDevice device, ControllerTimeView controllerTime) {
        this.device = device;
        this.controllerTime = controllerTime;
    }

    public VictronDevice getDevice() {
        return device;
    }

    public ControllerTimeView getControllerTime() {
        return controllerTime;
    }
}
