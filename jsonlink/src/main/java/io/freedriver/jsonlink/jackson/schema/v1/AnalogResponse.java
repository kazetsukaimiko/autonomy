package io.freedriver.jsonlink.jackson.schema.v1;

import java.util.Objects;

public class AnalogResponse {
    private Identifier pin;
    private Integer raw;
    private Float voltage;
    private Float resistance;

    public AnalogResponse() {
    }

    public Identifier getPin() {
        return pin;
    }

    public void setPin(Identifier pin) {
        this.pin = pin;
    }

    public Integer getRaw() {
        return raw;
    }

    public void setRaw(Integer raw) {
        this.raw = raw;
    }

    public Float getVoltage() {
        return voltage;
    }

    public void setVoltage(Float voltage) {
        this.voltage = voltage;
    }

    public Float getResistance() {
        return resistance;
    }

    public void setResistance(Float resistance) {
        this.resistance = resistance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AnalogResponse that = (AnalogResponse) o;
        return Objects.equals(pin, that.pin);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pin);
    }

    @Override
    public String toString() {
        return "AnalogResponse{" +
                "pin=" + pin +
                ", raw=" + raw +
                ", voltage=" + voltage +
                ", resistance=" + resistance +
                '}';
    }
}
