package io.freedriver.jsonlink.jackson.schema.v1;

import java.util.Objects;

public class AnalogRead {
    private Identifier pinNumber;
    private int resistance;

    public AnalogRead() {
    }

    public AnalogRead(Identifier pinNumber, int resistance) {
        this.pinNumber = pinNumber;
        this.resistance = resistance;
    }

    public Identifier getPinNumber() {
        return pinNumber;
    }

    public void setPinNumber(Identifier pinNumber) {
        this.pinNumber = pinNumber;
    }

    public int getResistance() {
        return resistance;
    }

    public void setResistance(int resistance) {
        this.resistance = resistance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AnalogRead that = (AnalogRead) o;
        return resistance == that.resistance &&
                Objects.equals(pinNumber, that.pinNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pinNumber, resistance);
    }
}
