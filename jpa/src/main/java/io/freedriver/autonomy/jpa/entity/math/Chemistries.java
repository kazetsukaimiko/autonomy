package io.freedriver.autonomy.jpa.entity.math;

import kaze.math.measurement.types.electrical.Potential;
import kaze.math.number.ScaledNumber;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static kaze.math.UnitPrefix.ONE;

public enum Chemistries {
    CHEM_18650(VoltageSoc.of(new Potential(ScaledNumber.of(3.2, ONE)), 0),
            VoltageSoc.of(new Potential(ScaledNumber.of(4.0, ONE)), 80),
            VoltageSoc.of(new Potential(ScaledNumber.of(4.2, ONE)), 100));

    private final Map<BigDecimal, BigDecimal> voltageMap;

    Chemistries(Map<BigDecimal, BigDecimal> voltageMap) {
        this.voltageMap = voltageMap;
    }

    Chemistries(VoltageSoc... VoltageSocs) {
        this(Stream.of(VoltageSocs)
                .collect(Collectors.toMap(VoltageSoc::getVoltageBase, VoltageSoc::getState, (a, b) -> b)));
    }

    public Map<BigDecimal, BigDecimal> getVoltageMap() {
        return voltageMap;
    }


}
