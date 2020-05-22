package io.freedriver.autonomy.jpa.converter.measurement;

import kaze.math.UnitPrefix;
import kaze.math.measurement.types.Measurement;

import javax.persistence.AttributeConverter;
import java.math.BigDecimal;

public abstract class MeasurementConverter<M extends Measurement<M>> implements AttributeConverter<M, Double> {
    public abstract M construct(BigDecimal value, UnitPrefix unitPrefix);

    @Override
    public Double convertToDatabaseColumn(M m) {
        return m.scaleTo(UnitPrefix.ONE).getValue()
                .getValue()
                .doubleValue();
    }

    @Override
    public M convertToEntityAttribute(Double value) {
        return construct(BigDecimal.valueOf(value), UnitPrefix.ONE)
                .normalize();
    }
}
