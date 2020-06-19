package io.freedriver.autonomy.jpa.converter.measurement;

import io.freedriver.math.TemporalUnit;
import io.freedriver.math.UnitPrefix;
import io.freedriver.math.measurement.types.TemporalMeasurement;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class StringTemporalMeasurementConverter<TM extends TemporalMeasurement<TM>> extends StringMeasurementConverter<TM> {
    protected static final String TEMPORAL_GROUP = "temporal";

    @Override
    public TM convertToEntityAttribute(String s) {
        Matcher m = Pattern.compile(BASE_PATTERN_STRING + unit().getSymbol() + "(?<" + TEMPORAL_GROUP + ">[a-zA-Z]+)")
                .matcher(s);
        if (m.matches()) {
            BigDecimal value = new BigDecimal(m.group(VALUE_GROUP));
            Optional<UnitPrefix> multiplier = UnitPrefix.ofPrefix(m.group(MULTIPLIER_GROUP));
            Optional<TemporalUnit> temporalUnit = TemporalUnit.bySuffix(m.group(TEMPORAL_GROUP));
            if (multiplier.isPresent() && temporalUnit.isPresent()) {
                return construct(value, multiplier.get(), temporalUnit.get());
            } else if (m.group(MULTIPLIER_GROUP) == null && temporalUnit.isPresent()) {
                return construct(value, UnitPrefix.ONE, temporalUnit.get());
            }
        }
        return null;
    }

    public abstract TM construct(BigDecimal value, UnitPrefix unitPrefix, TemporalUnit temporalUnit);
}
