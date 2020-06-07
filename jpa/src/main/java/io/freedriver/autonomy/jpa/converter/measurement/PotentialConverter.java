package io.freedriver.autonomy.jpa.converter.measurement;

import kaze.math.UnitPrefix;
import kaze.math.measurement.types.electrical.Potential;
import kaze.math.number.ScaledNumber;

import javax.persistence.Converter;
import java.math.BigDecimal;

@Converter(autoApply = true)
public class PotentialConverter extends MeasurementConverter<Potential> {
    @Override
    public Potential construct(BigDecimal value, UnitPrefix unitPrefix) {
        return new Potential(ScaledNumber.of(value, unitPrefix));
    }
}
