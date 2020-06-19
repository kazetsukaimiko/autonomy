package io.freedriver.autonomy.jpa.converter.measurement;

import io.freedriver.math.measurement.types.electrical.Current;

public class CurrentConverterTest extends MeasurementConverterTest<Current, CurrentConverter> {
    @Override
    public CurrentConverter spawn() {
        return new CurrentConverter();
    }
}
