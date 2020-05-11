package io.freedriver.autonomy.jpa.converter.measurement;

import kaze.math.measurement.units.Current;

public class CurrentConverterTest extends MeasurementConverterTest<Current, CurrentConverter> {
    @Override
    public CurrentConverter spawn() {
        return new CurrentConverter();
    }
}
