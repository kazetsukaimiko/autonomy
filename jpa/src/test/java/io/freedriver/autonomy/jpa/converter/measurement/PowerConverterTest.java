package io.freedriver.autonomy.jpa.converter.measurement;

import kaze.math.measurement.units.Power;

public class PowerConverterTest extends MeasurementConverterTest<Power, PowerConverter> {
    @Override
    public PowerConverter spawn() {
        return new PowerConverter();
    }
}
