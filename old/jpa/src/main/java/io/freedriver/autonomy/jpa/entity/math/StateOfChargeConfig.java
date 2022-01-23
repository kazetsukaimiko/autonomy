package io.freedriver.autonomy.jpa.entity.math;

import io.freedriver.math.measurement.types.electrical.Potential;
import io.freedriver.math.number.ScaledNumber;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

import static io.freedriver.math.UnitPrefix.ONE;

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
                .map(e -> VoltageSoc.of(new Potential(ScaledNumber.of(e.getKey(), ONE)), e.getValue()))
                .map(voltageSoc -> voltageSoc.series(cells))
                .sorted(Comparator.comparing(v -> v.getVoltage().subtract(voltage.getValue()).abs()))
                .limit(2)
                .collect(Collectors.toList()))
                .calculate(voltage);
    }
}
