package io.freedriver.autonomy.jpa.converter.measurement;

import kaze.math.UnitPrefix;
import kaze.math.measurement.types.Measurement;
import kaze.math.measurement.units.SIElectricalUnit;

import javax.persistence.AttributeConverter;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class StringMeasurementConverter<M extends Measurement<M>> implements AttributeConverter<M, String> {
    protected static final String VALUE_GROUP = "value";
    protected static final String MULTIPLIER_GROUP = "multiplier";
    protected static final String BASE_PATTERN_STRING = "(?<" + VALUE_GROUP + ">[0-9]+(\\.[0-9]+)?)\\s+(?<" + MULTIPLIER_GROUP + ">[a-zA-Z]+)?";

    public abstract BiFunction<BigDecimal, UnitPrefix, M> constructor();

    public abstract SIElectricalUnit unit();

    @Override
    public final String convertToDatabaseColumn(M m) {
        return m.toString();
    }

    @Override
    public M convertToEntityAttribute(String s) {
        Matcher m = Pattern.compile(BASE_PATTERN_STRING + unit().getSymbol())
                .matcher(s);
        if (m.matches()) {
            Optional<UnitPrefix> multiplier = UnitPrefix.ofPrefix(m.group(MULTIPLIER_GROUP));
            if (multiplier.isPresent()) {
                return constructor().apply(new BigDecimal(m.group(VALUE_GROUP)), multiplier.get());
            } else if (m.group(MULTIPLIER_GROUP) == null) {
                return constructor().apply(new BigDecimal(m.group(VALUE_GROUP)), UnitPrefix.ONE);
            }
        }
        return null;
    }
}
