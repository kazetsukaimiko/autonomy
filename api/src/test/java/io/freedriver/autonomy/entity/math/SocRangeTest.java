package io.freedriver.autonomy.entity.math;

import kaze.math.measurement.units.Potential;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.stream.IntStream;

import static kaze.math.Multiplier.BASE;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SocRangeTest {

    @Test
    public void testSocRange() {
        SocRange scaled = SocRange.of(VoltageSoc.of(Potential.of(0, BASE), 0), VoltageSoc.of(Potential.of(100, BASE),100));
        IntStream.range(0, 100)
                .boxed()
                .map(BigDecimal::valueOf)
                .forEach(i -> assertBDEquals(i, scaled.calculate(Potential.of(i, BASE))));
    }

    @Test
    public void testSocRangeHappyPath() {
        SocRange known = SocRange.of(
                VoltageSoc.of(Potential.of(3.2, BASE), 0),
                VoltageSoc.of(Potential.of(4.2, BASE), 100)
        );

        assertBDEquals(new BigDecimal("0.0"), known.calculate(Potential.of(3.2, BASE)));
        assertBDEquals(new BigDecimal("50.0"), known.calculate(Potential.of(3.7, BASE)));
        assertBDEquals(new BigDecimal("100.0"), known.calculate(Potential.of(4.2, BASE)));
    }


    public static void assertBDEquals(BigDecimal a, BigDecimal b) {
        if (a.scale() > b.scale()) {
            assertEquals(a, b.setScale(a.scale(), RoundingMode.UNNECESSARY), "Scaled values match");
        } else {
            assertEquals(a.setScale(b.scale(), RoundingMode.UNNECESSARY), b, "Scaled values match");
        }
    }
}
