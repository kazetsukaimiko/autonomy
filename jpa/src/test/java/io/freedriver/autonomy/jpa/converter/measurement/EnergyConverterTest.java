package io.freedriver.autonomy.jpa.converter.measurement;

import kaze.math.measurement.units.Energy;

public class EnergyConverterTest extends TemporalMeasurementConverterTest<Energy, EnergyConverter> {
    @Override
    public EnergyConverter spawn() {
        return new EnergyConverter();
    }
}