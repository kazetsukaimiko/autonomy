package io.freedriver.autonomy.entity.math;

import io.freedriver.autonomy.jpa.entity.math.SocRange;
import io.freedriver.autonomy.jpa.entity.math.VoltageSoc;
import kaze.math.measurement.units.Potential;
import kaze.math.number.ScaledNumber;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.stream.IntStream;

import static kaze.math.UnitPrefix.ONE;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SocRangeTest {

    @Test
    public void testSocRange() {
        SocRange scaled = SocRange.of(VoltageSoc.of(new Potential(ScaledNumber.of(0, ONE)), 0), VoltageSoc.of(new Potential(ScaledNumber.of(100, ONE)),100));
        IntStream.range(0, 100)
                .boxed()
                .map(BigDecimal::valueOf)
                .forEach(i -> assertBDEquals(i, scaled.calculate(new Potential(ScaledNumber.of(i, ONE)))));
    }

    @Test
    public void testSocRangeHappyPath() {
        SocRange known = SocRange.of(
                VoltageSoc.of(new Potential(ScaledNumber.of(3.2, ONE)), 0),
                VoltageSoc.of(new Potential(ScaledNumber.of(4.2, ONE)), 100)
        );

        assertBDEquals(new BigDecimal("0.0"), known.calculate(new Potential(ScaledNumber.of(3.2, ONE))));
        assertBDEquals(new BigDecimal("50.0"), known.calculate(new Potential(ScaledNumber.of(3.7, ONE))));
        assertBDEquals(new BigDecimal("100.0"), known.calculate(new Potential(ScaledNumber.of(4.2, ONE))));
    }


    public static void assertBDEquals(BigDecimal a, BigDecimal b) {
        if (a.scale() > b.scale()) {
            assertEquals(a, b.setScale(a.scale(), RoundingMode.UNNECESSARY), "Scaled values match");
        } else {
            assertEquals(a.setScale(b.scale(), RoundingMode.UNNECESSARY), b, "Scaled values match");
        }
    }
}
