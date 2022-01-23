package io.freedriver.autonomy.entity.view;

import io.freedriver.autonomy.jpa.entity.VEDirectMessage;

public class ControllerStateView {
    private final VEDirectMessage lastMessage;

    public ControllerStateView(VEDirectMessage lastMessage) {
        this.lastMessage = lastMessage;
    }

    public Double getYield() {
        return lastMessage.getYieldToday().doubleValue();
    }

    public Double getMainVoltage() {
        return lastMessage.getMainVoltage().doubleValue();
    }

    public Double getPanelVoltage() {
        return lastMessage.getPanelVoltage().doubleValue();
    }

    public Double getPanelPower() {
        return lastMessage.getPanelPower().doubleValue();
    }

    public Double getMaxPanelPower() {
        return lastMessage.getMaxPowerToday().doubleValue();
    }
}
