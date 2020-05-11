package io.freedriver.autonomy.jpa.entity.math;

import kaze.math.UnitPrefix;
import kaze.math.measurement.units.Potential;

import java.math.BigDecimal;

public final class VoltageSoc {
    private final Potential voltage;
    private final BigDecimal state;

    private VoltageSoc(Potential voltage, BigDecimal state) {
        this.voltage = voltage;
        this.state = state;
    }

    public static VoltageSoc of(Potential voltage, BigDecimal state) {
        return new VoltageSoc(voltage, state);
    }

    public static VoltageSoc of(Potential voltage, Number state) {
        return new VoltageSoc(voltage, BigDecimal.valueOf(state.doubleValue()));
    }

    public Potential getVoltage() {
        return voltage;
    }

    public BigDecimal getState() {
        return state;
    }

    public BigDecimal getVoltageBase() {
        return getVoltage().scaleTo(UnitPrefix.ONE)
                .getValue()
                .getValue();
    }

    public VoltageSoc series(int cells) {
        return new VoltageSoc(voltage.multiply(cells), state);
    }
}