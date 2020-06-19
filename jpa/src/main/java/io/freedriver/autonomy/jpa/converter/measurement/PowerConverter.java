package io.freedriver.autonomy.jpa.converter.measurement;

import io.freedriver.math.UnitPrefix;
import io.freedriver.math.measurement.types.electrical.Power;
import io.freedriver.math.number.ScaledNumber;

import javax.persistence.Converter;
import java.math.BigDecimal;

@Converter(autoApply = true)
public class PowerConverter extends MeasurementConverter<Power> {
    @Override
    public Power construct(BigDecimal value, UnitPrefix unitPrefix) {
        return new Power(ScaledNumber.of(value, unitPrefix));
    }
}
