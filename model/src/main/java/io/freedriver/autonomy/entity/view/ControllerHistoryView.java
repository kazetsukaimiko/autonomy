package io.freedriver.autonomy.entity.view;

import lombok.Builder;

@Builder(toBuilder = true)
public record ControllerHistoryView(
        double maxPanelPower,
        double maxPanelVoltage,
        double maxMainVoltage,
        double recordYield) {}
