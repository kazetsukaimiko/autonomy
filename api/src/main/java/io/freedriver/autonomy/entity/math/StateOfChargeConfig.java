package io.freedriver.autonomy.entity.math;

import kaze.math.measurement.units.Potential;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

import static kaze.math.Multiplier.BASE;

// 3.2 -> 4.2
// 19.2 -> 25.2

public class StateOfChargeConfig {
    private int cells = 12;
    private Map<BigDecimal, BigDecimal> voltages = Chemistries.CHEM_18650.getVoltageMap();

    public StateOfChargeConfig() {
    }

    public int getCells() {
        return cells;
    }

    public void setCells(int cells) {
        this.cells = cells;
    }

    public Map<BigDecimal, BigDecimal> getVoltages() {
        return voltages;
    }

    public void setVoltages(Map<BigDecimal, BigDecimal> voltages) {
        this.voltages = voltages;
    }

    public BigDecimal calculate(Potential voltage) {
        return SocRange.of(voltages.entrySet()
                .stream()
                .map(e -> VoltageSoc.of(Potential.of(e.getKey(), BASE), e.getValue()))
                .sorted(Comparator.comparing(v -> v.getVoltage().subtract(voltage.divide(cells)).abs()))
                .limit(2)
                .collect(Collectors.toList()))
                .calculate(voltage);
    }
}
