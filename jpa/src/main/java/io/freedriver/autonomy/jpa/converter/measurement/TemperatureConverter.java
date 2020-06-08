package io.freedriver.autonomy.jpa.converter.measurement;

import kaze.math.UnitPrefix;
import kaze.math.measurement.types.thermo.Temperature;
import kaze.math.measurement.units.TemperatureScale;
import kaze.math.number.ScaledNumber;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.math.BigDecimal;

@Converter(autoApply = true)
public class TemperatureConverter extends MeasurementConverter<Temperature> implements AttributeConverter<Temperature, BigDecimal> {
    @Override
    public Temperature construct(BigDecimal value, UnitPrefix unitPrefix) {
        return new Temperature(ScaledNumber.of(value, unitPrefix), TemperatureScale.CELSUIS);
    }
}
