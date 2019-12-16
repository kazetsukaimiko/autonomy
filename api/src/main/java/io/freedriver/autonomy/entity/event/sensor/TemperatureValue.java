package io.freedriver.autonomy.entity.event.sensor;

import java.math.BigDecimal;
import java.util.Objects;

import static io.freedriver.autonomy.entity.event.sensor.TemperatureUnit.KELVIN;

public class TemperatureValue implements Comparable<TemperatureValue> {
    private static final BigDecimal EQUIVALENCE = new BigDecimal("0.000000000001");

    private BigDecimal value;
    private TemperatureUnit unit;

    public TemperatureValue(BigDecimal value, TemperatureUnit unit) {
        this.value = value;
        this.unit = unit;
    }

    public TemperatureValue to(TemperatureUnit newUnit) {
        return new TemperatureValue(
                newUnit.convert(value, unit),
                newUnit
        );
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public TemperatureUnit getUnit() {
        return unit;
    }

    public void setUnit(TemperatureUnit unit) {
        this.unit = unit;
    }

    public TemperatureValue convert(TemperatureUnit unit) {
        return new TemperatureValue(unit.convert(this), unit);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TemperatureValue that = (TemperatureValue) o;
        return EQUIVALENCE.compareTo(
                KELVIN.convert(this).min(KELVIN.convert(that)).abs()
        ) > 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, unit);
    }

    @Override
    public int compareTo(TemperatureValue temperatureValue) {
        return KELVIN.convert(this).compareTo(KELVIN.convert(temperatureValue));
    }
}
