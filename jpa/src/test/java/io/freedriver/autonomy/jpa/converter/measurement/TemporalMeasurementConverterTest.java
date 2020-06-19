package io.freedriver.autonomy.jpa.converter.measurement;

import io.freedriver.math.TemporalUnit;
import io.freedriver.math.UnitPrefix;
import io.freedriver.math.measurement.types.TemporalMeasurement;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class TemporalMeasurementConverterTest<TM extends TemporalMeasurement<TM>, TMC extends TemporalMeasurementConverter<TM>> extends MeasurementConverterTest<TM, TMC> {

    @Test
    public void testScalingDifferences() {
        assertConversionBidirectionality(victim.construct(BigDecimal.ONE, UnitPrefix.NANO));
    }


    public void assertConversionBidirectionality(TM sample) {
        BigDecimal representation = victim.convertToDatabaseColumn(sample);
        TM replica = victim.convertToEntityAttribute(representation);
        System.out.println(sample + " vs " + replica);
        assertEquals(sample, replica, "Replica should match");
    }

    @Test
    public void testAllWithTemporal() {
        TemporalUnit.stream()
                .forEach(temporalUnit -> UnitPrefix.stream()
                        .forEach(multiplier -> stream()
                                .forEach(value -> assertConversionBidirectionality(
                                        victim.construct(value, multiplier)))));
    }
}
