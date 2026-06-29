package io.freedriver.autonomy.jpa.entity.math;

import java.math.BigDecimal;
import java.util.List;

import io.freedriver.math.measurement.types.electrical.Potential;

public class SocRange {
    private final VoltageSoc bottom;
    private final VoltageSoc top;

    private SocRange(VoltageSoc bottom, VoltageSoc top) {
        this.bottom = bottom;
        this.top = top;
    }

    public static SocRange of(VoltageSoc bottom, VoltageSoc top) {
        if (bottom.voltage().lessThan(top.voltage())) {
            return new SocRange(bottom, top);
        }
        return new SocRange(top, bottom);
    }

    public static SocRange of(List<VoltageSoc> socs) {
        return of(socs.get(0), socs.get(1));
    }

    public BigDecimal calculate(Potential voltage) {
        Potential range = top.voltage().subtract(bottom.voltage().getValue());
        BigDecimal scale = voltage.subtract(bottom.voltage()).divide(range).getValue().getValue();
        BigDecimal socRange = top.state().subtract(bottom.state());
        BigDecimal result = scale.multiply(socRange);
        return result;
    }
}
