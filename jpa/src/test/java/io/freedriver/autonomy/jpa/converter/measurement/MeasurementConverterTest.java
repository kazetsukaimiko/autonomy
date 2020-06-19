package io.freedriver.autonomy.jpa.converter.measurement;

import io.freedriver.math.UnitPrefix;
import io.freedriver.math.measurement.types.Measurement;
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

    @Test
    public void testRaw() {
        stream()
                .forEach(value -> {
                    M replica = victim.convertToEntityAttribute(value);
                    BigDecimal representation = victim.convertToDatabaseColumn(replica);
                    //assertEquals(value, representation);
                    assertEquals(replica, victim.convertToEntityAttribute(representation));
                });
    }

    public static Stream<BigDecimal> stream() {
        return Stream.of(
                0d,
                1d,
                10d,
                0.34453d,
                10293123.131131d
        ).map(BigDecimal::valueOf);
    }

    public final M construct(BigDecimal value, UnitPrefix unitPrefix) {
        return victim.construct(value, unitPrefix);
    }

}
