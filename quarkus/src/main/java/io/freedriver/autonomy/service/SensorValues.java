package io.freedriver.autonomy.service;

public class SensorValues {
    private int min = 0;
    private int raw = 0;
    private int max = 0;

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

    public SensorValues apply(int value) {
        if (value > max) {
            max = value;
        }
        if (value < min) {
            min = value;
        }
        raw = value;
        return this;
    }
}
