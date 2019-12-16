package io.freedriver.autonomy.entity.event.sensor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.function.Function;

public enum TemperatureUnit {
    KELVIN("K", Function.identity(), Function.identity()),
    FAHRENHEIT("F", Converters.F_TO_K, Converters.K_TO_F),
    CELSIUS("C", Converters.C_TO_K, Converters.K_TO_C);

    private final String suffix;
    private final Function<BigDecimal, BigDecimal> toKelvin;
    private final Function<BigDecimal, BigDecimal> fromKelvin;

    TemperatureUnit(String suffix, Function<BigDecimal, BigDecimal> toKelvin, Function<BigDecimal, BigDecimal> fromKelvin) {
        this.suffix = suffix;
        this.toKelvin = toKelvin;
        this.fromKelvin = fromKelvin;
    }

    public String getSuffix() {
        return suffix;
    }

    public Function<BigDecimal, BigDecimal> getToKelvin() {
        return toKelvin;
    }

    public Function<BigDecimal, BigDecimal> getFromKelvin() {
        return fromKelvin;
    }

    public BigDecimal convert(BigDecimal value, TemperatureUnit oldUnit) {
        return Optional.ofNullable(value)
                .map(oldUnit.toKelvin)
                .map(fromKelvin)
                .orElse(null);
    }

    public BigDecimal convert(TemperatureValue temperatureValue) {
        return convert(temperatureValue.getValue(), temperatureValue.getUnit());
    }

    private static final class Converters {
        static final BigDecimal F_K_CONST = new BigDecimal("459.67");
        static final BigDecimal C_K_CONST = new BigDecimal("273.15");
        static final BigDecimal FIVE_NINTHS = new BigDecimal("5.0").divide(new BigDecimal("9.0"), RoundingMode.UNNECESSARY);
        static final BigDecimal NINE_FIFTHS = new BigDecimal("9.0").divide(new BigDecimal("5.0"), RoundingMode.UNNECESSARY);

        static final Function<BigDecimal, BigDecimal> F_TO_K =
                f -> f.add(F_K_CONST).multiply(FIVE_NINTHS);
        static final Function<BigDecimal, BigDecimal> K_TO_F =
                k -> k.multiply(NINE_FIFTHS).min(F_K_CONST);
        static final Function<BigDecimal, BigDecimal> C_TO_K =
                c -> c.add(C_K_CONST);
        static final Function<BigDecimal, BigDecimal> K_TO_C =
                k -> k.min(C_K_CONST);
    }
}
