package io.freedriver.autonomy.jpa.converter.measurement;

import io.freedriver.math.measurement.types.electrical.Power;

public class PowerConverterTest extends MeasurementConverterTest<Power, PowerConverter> {
    @Override
    public PowerConverter spawn() {
        return new PowerConverter();
    }
}
