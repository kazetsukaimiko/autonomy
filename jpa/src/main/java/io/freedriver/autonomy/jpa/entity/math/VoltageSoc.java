package io.freedriver.autonomy.jpa.entity.math;

import java.math.BigDecimal;

import io.freedriver.math.UnitPrefix;
import io.freedriver.math.measurement.types.electrical.Potential;

public record VoltageSoc(Potential voltage, BigDecimal state) {

    public static VoltageSoc of(Potential voltage, BigDecimal state) {
        return new VoltageSoc(voltage, state);
    }

    public static VoltageSoc of(Potential voltage, Number state) {
        return new VoltageSoc(voltage, BigDecimal.valueOf(state.doubleValue()));
    }

    public BigDecimal getVoltageBase() {
        return voltage().scaleTo(UnitPrefix.ONE)
                .getValue()
                .getValue();
    }

    public VoltageSoc series(int cells) {
        return new VoltageSoc(voltage.multiply(cells), state);
    }
}