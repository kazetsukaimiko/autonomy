package io.freedriver.autonomy.entity.view;

import io.freedriver.autonomy.jpa.entity.VEDirectMessage;
import lombok.Builder;

@Builder(toBuilder = true)
public record ControllerStateView(VEDirectMessage lastMessage) {

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
