package io.freedriver.autonomy.jpa.converter.measurement;

import kaze.math.UnitPrefix;
import kaze.math.measurement.types.electrical.Energy;
import kaze.math.number.ScaledNumber;

import javax.persistence.Converter;
import java.math.BigDecimal;

@Converter(autoApply = true)
public class EnergyConverter extends TemporalMeasurementConverter<Energy> {

    @Override
    public Energy construct(BigDecimal value, UnitPrefix unitPrefix) {
        return new Energy(ScaledNumber.of(value, unitPrefix));
    }
}
