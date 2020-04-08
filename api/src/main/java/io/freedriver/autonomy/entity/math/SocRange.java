package io.freedriver.autonomy.entity.math;

import kaze.math.measurement.units.Potential;

import java.math.BigDecimal;
import java.util.List;

public class SocRange {
    private final VoltageSoc bottom;
    private final VoltageSoc top;

    private SocRange(VoltageSoc bottom, VoltageSoc top) {
        this.bottom = bottom;
        this.top = top;
    }

    public static SocRange of(VoltageSoc bottom, VoltageSoc top) {
        if (bottom.getVoltage().lessThan(top.getVoltage())) {
            return new SocRange(bottom, top);
        }
        return new SocRange(top, bottom);
    }

    public static SocRange of(List<VoltageSoc> socs) {
        return of(socs.get(0), socs.get(1));
    }

    public BigDecimal calculate(Potential voltage) {
        Potential range = top.getVoltage().subtract(bottom.getVoltage());
        BigDecimal scale = voltage.subtract(bottom.getVoltage()).divide(range);
        BigDecimal socRange = top.getState().subtract(bottom.getState());
        BigDecimal result = scale.multiply(socRange);
        return result;
    }
}
