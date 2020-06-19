package io.freedriver.autonomy.jpa.converter.measurement;

import io.freedriver.math.UnitPrefix;
import io.freedriver.math.measurement.types.electrical.Energy;
import io.freedriver.math.number.ScaledNumber;

import javax.persistence.Converter;
import java.math.BigDecimal;

@Converter(autoApply = true)
public class EnergyConverter extends TemporalMeasurementConverter<Energy> {

    @Override
    public Energy construct(BigDecimal value, UnitPrefix unitPrefix) {
        return new Energy(ScaledNumber.of(value, unitPrefix));
    }
}
