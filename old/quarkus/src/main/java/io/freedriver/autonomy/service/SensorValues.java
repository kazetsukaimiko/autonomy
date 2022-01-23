package io.freedriver.autonomy.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

public class SensorValues {
    private Instant recordedOn = Instant.now();
    private int min = -1;
    private int raw = -1;
    private int max = -1;
    private double percentage = -1;

    public SensorValues() {
    }

    public Instant getRecordedOn() {
        return recordedOn;
    }

    public void setRecordedOn(Instant recordedOn) {
        this.recordedOn = recordedOn;
    }

    public int getMin() {
        return min;
    }

    public void setMin(int min) {
        this.min = min;
    }

    public int getRaw() {
        return raw;
    }

    public void setRaw(int raw) {
        this.raw = raw;
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }
    // 267 : 260, 1000
    // 267 : 0, 267
    // 267 :

    public SensorValues apply(int value) {
        if (value > max || max == -1) {
            max = value;
        }
        if (value < min || min == -1) {
            min = value;
        }
        raw = value;
        if (min != -1 && max != -1 && value != -1 && (max - min) > 0) {
            percentage = BigDecimal.valueOf(((double)value - (double)min) / ((double)max - (double)min))
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP)
                    .doubleValue();
        }
        return this;
    }
}
