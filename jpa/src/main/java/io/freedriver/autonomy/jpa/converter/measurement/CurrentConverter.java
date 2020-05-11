package io.freedriver.autonomy.jpa.converter.measurement;

import kaze.math.UnitPrefix;
import kaze.math.measurement.units.Current;
import kaze.math.number.ScaledNumber;

import javax.persistence.Converter;
import java.math.BigDecimal;

@Converter(autoApply = true)
public class CurrentConverter extends MeasurementConverter<Current> {
    @Override
    public Current construct(BigDecimal value, UnitPrefix unitPrefix) {
        return new Current(ScaledNumber.of(value, unitPrefix));
    }
}
