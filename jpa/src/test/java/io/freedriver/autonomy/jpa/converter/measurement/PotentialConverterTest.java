package io.freedriver.autonomy.jpa.converter.measurement;

import kaze.math.measurement.units.Potential;

public class PotentialConverterTest extends MeasurementConverterTest<Potential, PotentialConverter> {
    @Override
    public PotentialConverter spawn() {
        return new PotentialConverter();
    }
}
