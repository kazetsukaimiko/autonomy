package io.freedriver.autonomy.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

import lombok.Builder;

@Builder(toBuilder = true)
public record SensorValues(
        Instant recordedOn,
        int min,
        int raw,
        int max,
        double percentage) {

    public SensorValues {
        if (recordedOn == null) {
            recordedOn = Instant.now();
        }
    }

    public SensorValues() {
        this(Instant.now(), -1, -1, -1, -1);
    }

    public SensorValues apply(int value) {
        int nextMax = (value > max || max == -1) ? value : max;
        int nextMin = (value < min || min == -1) ? value : min;
        double nextPercentage = percentage;
        if (nextMin != -1 && nextMax != -1 && value != -1 && (nextMax - nextMin) > 0) {
            nextPercentage = BigDecimal.valueOf(((double) value - (double) nextMin) / ((double) nextMax - (double) nextMin))
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP)
                    .doubleValue();
        }
        return toBuilder()
                .min(nextMin)
                .raw(value)
                .max(nextMax)
                .percentage(nextPercentage)
                .build();
    }
}
