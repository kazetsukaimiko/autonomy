package io.freedriver.autonomy.entity.view;

import kaze.victron.VictronDevice;

public class ControllerView {
    private final VictronDevice device;

    private final ControllerTimeView controllerTimeView;

    public ControllerView(VictronDevice device, ControllerTimeView controllerTimeView) {
        this.device = device;
        this.controllerTimeView = controllerTimeView;
    }


    public ControllerTimeView getControllerTimeView() {
        return controllerTimeView;
    }
}
