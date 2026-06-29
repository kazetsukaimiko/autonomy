package io.freedriver.autonomy.entity.view;

public record ControllerHistoryView(
        double maxPanelPower,
        double maxPanelVoltage,
        double maxMainVoltage,
        double recordYield) {}