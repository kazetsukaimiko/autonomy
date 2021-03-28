package io.freedriver.autonomy.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class SensorValues {
    private int min = -1;
    private int raw = -1;
    private int max = -1;
    private float percentage = -1;

    public SensorValues() {
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

    public float getPercentage() {
        return percentage;
    }

    public void setPercentage(float percentage) {
        this.percentage = percentage;
    }

    public SensorValues apply(int value) {
        if (value > max || max == -1) {
            max = value;
        }
        if (value < min || min == -1) {
            min = value;
        }
        raw = value;
        if (min != -1 && max != -1 && value != -1 && (max - min) > 0) {
            percentage = BigDecimal.valueOf(((float)value - (float)min) / ((float)max - (float)min))
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP)
                    .floatValue();
        }
        return this;
    }
}
