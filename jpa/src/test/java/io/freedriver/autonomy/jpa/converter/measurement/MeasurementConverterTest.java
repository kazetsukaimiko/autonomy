package io.freedriver.autonomy.jpa.converter.measurement;

import kaze.math.UnitPrefix;
import kaze.math.measurement.types.Measurement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class MeasurementConverterTest<M extends Measurement<M>, MC extends MeasurementConverter<M>> {

    public abstract MC spawn();

    MC victim;

    @BeforeEach
    public void init() {
        victim = spawn();
    }

    @Test
    public void testAll() {
        UnitPrefix.stream()
            .forEach(multiplier -> stream()
                .forEach(value -> {
                    M sample = construct(value, multiplier);
                    BigDecimal representation = victim.convertToDatabaseColumn(sample);
                    M replica = victim.convertToEntityAttribute(representation);
                    assertEquals(sample, replica, "Replica should match.");
                }));
    }

    public static Stream<BigDecimal> stream() {
        return Stream.of(
                BigDecimal.ZERO,
                BigDecimal.ONE,
                BigDecimal.TEN,
                new BigDecimal("0.34453"),
                new BigDecimal("10293123.131131")
        );
    }

    public final M construct(BigDecimal value, UnitPrefix unitPrefix) {
        return victim.construct(value, unitPrefix);
    }

}
