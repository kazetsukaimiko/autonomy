package io.freedriver.autonomy.jpa.converter.measurement;

import kaze.math.UnitPrefix;
import kaze.math.measurement.types.TemporalMeasurement;

import java.math.BigDecimal;

public abstract class TemporalMeasurementConverter<TM extends TemporalMeasurement<TM>> extends MeasurementConverter<TM> {
    @Override
    public Double convertToDatabaseColumn(TM tm) {
        return tm.scaleTo(UnitPrefix.ONE).getValue()
                .getValue()
                .doubleValue();
    }

    @Override
    public TM convertToEntityAttribute(Double value) {
        return construct(BigDecimal.valueOf(value), UnitPrefix.ONE)
                .normalize();
    }

    public abstract TM construct(BigDecimal value, UnitPrefix unitPrefix);
}
