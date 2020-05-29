package io.freedriver.autonomy.entity.view;

public class ControllerHistoryView {
    private final double maxPanelPower;
    private final double maxPanelVoltage;
    private final double maxMainVoltage;
    private final double recordYield;

    public ControllerHistoryView(double maxPanelPower, double maxPanelVoltage, double maxMainVoltage, double recordYield) {
        this.maxPanelPower = maxPanelPower;
        this.maxPanelVoltage = maxPanelVoltage;
        this.maxMainVoltage = maxMainVoltage;
        this.recordYield = recordYield;
    }

    public double getMaxPanelPower() {
        return maxPanelPower;
    }

    public double getMaxPanelVoltage() {
        return maxPanelVoltage;
    }

    public double getMaxMainVoltage() {
        return maxMainVoltage;
    }

    public double getRecordYield() {
        return recordYield;
    }
}

