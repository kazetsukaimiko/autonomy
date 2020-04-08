package io.freedriver.autonomy.entity.math;

import kaze.math.measurement.units.Potential;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static kaze.math.Multiplier.BASE;

public enum Chemistries {
    CHEM_18650(VoltageSoc.of(Potential.of(3.2, BASE), 0), VoltageSoc.of(Potential.of(4.2, BASE), 100));

    private final Map<Potential, BigDecimal> voltageMap;

    Chemistries(Map<Potential, BigDecimal> voltageMap) {
        this.voltageMap = voltageMap;
    }

    Chemistries(VoltageSoc... VoltageSocs) {
        this(Stream.of(VoltageSocs)
                .collect(Collectors.toMap(VoltageSoc::getVoltage, VoltageSoc::getState, (a, b) -> b)));
    }

    public Map<Potential, BigDecimal> getVoltageMap() {
        return voltageMap;
    }


}
