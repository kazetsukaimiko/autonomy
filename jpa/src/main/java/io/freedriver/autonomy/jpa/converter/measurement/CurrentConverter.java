package io.freedriver.autonomy.jpa.converter.measurement;

import io.freedriver.math.UnitPrefix;
import io.freedriver.math.measurement.types.electrical.Current;
import io.freedriver.math.number.ScaledNumber;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.math.BigDecimal;

@Converter(autoApply = true)
public class CurrentConverter extends MeasurementConverter<Current> implements AttributeConverter<Current, BigDecimal> {
    @Override
    public Current construct(BigDecimal value, UnitPrefix unitPrefix) {
        return new Current(ScaledNumber.of(value, unitPrefix));
    }
}
