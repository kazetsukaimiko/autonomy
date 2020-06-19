package io.freedriver.autonomy.jpa.converter.measurement;

import io.freedriver.math.measurement.types.electrical.Energy;

public class EnergyConverterTest extends TemporalMeasurementConverterTest<Energy, EnergyConverter> {
    @Override
    public EnergyConverter spawn() {
        return new EnergyConverter();
    }
}
