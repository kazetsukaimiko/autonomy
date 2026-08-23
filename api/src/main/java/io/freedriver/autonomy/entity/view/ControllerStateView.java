package io.freedriver.autonomy.entity.view;

import io.freedriver.autonomy.jpa.entity.VEDirectMessage;
import lombok.Builder;

@Builder(toBuilder = true)
public record ControllerStateView(VEDirectMessage lastMessage) {

    public Double getYield() {
        return lastMessage.yieldToday().doubleValue();
    }

    public Double getMainVoltage() {
        return lastMessage.mainVoltage().doubleValue();
    }

    public Double getPanelVoltage() {
        return lastMessage.panelVoltage().doubleValue();
    }

    public Double getPanelPower() {
        return lastMessage.panelPower().doubleValue();
    }

    public Double getMaxPanelPower() {
        return lastMessage.maxPowerToday().doubleValue();
    }
}
