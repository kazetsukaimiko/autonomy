package io.freedriver.autonomy.jpa.entity.math;

import static io.freedriver.math.UnitPrefix.ONE;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

import io.freedriver.math.measurement.types.electrical.Potential;
import io.freedriver.math.number.ScaledNumber;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

// 3.2 -> 4.2
// 19.2 -> 25.2

@Getter
@Setter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
public class StateOfChargeConfig {
    private int cells = 12;
    private Map<BigDecimal, BigDecimal> voltages = Chemistries.CHEM_18650.getVoltageMap();

    public BigDecimal calculate(Potential voltage) {
        return SocRange.of(voltages.entrySet()
                .stream()
                .map(e -> VoltageSoc.of(new Potential(ScaledNumber.of(e.getKey(), ONE)), e.getValue()))
                .map(voltageSoc -> voltageSoc.series(cells))
                .sorted(Comparator.comparing(v -> v.voltage().subtract(voltage.getValue()).abs()))
                .limit(2)
                .collect(Collectors.toList()))
                .calculate(voltage);
    }
}
