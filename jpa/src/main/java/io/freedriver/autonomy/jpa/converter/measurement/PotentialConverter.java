package io.freedriver.autonomy.jpa.converter.measurement;

import io.freedriver.math.UnitPrefix;
import io.freedriver.math.measurement.types.electrical.Potential;
import io.freedriver.math.number.ScaledNumber;

import javax.persistence.Converter;
import java.math.BigDecimal;

@Converter(autoApply = true)
public class PotentialConverter extends MeasurementConverter<Potential> {
    @Override
    public Potential construct(BigDecimal value, UnitPrefix unitPrefix) {
        return new Potential(ScaledNumber.of(value, unitPrefix));
    }
}
