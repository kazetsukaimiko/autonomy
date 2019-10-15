package io.freedriver.jsonlink.jackson.schema.v1;

import java.util.Objects;

public class ModeSet {
    private Identifier pinNumber;
    private Mode mode;

    public ModeSet() {
    }

    public ModeSet(Identifier pinNumber, Mode mode) {
        this.pinNumber = pinNumber;
        this.mode = mode;
    }

    public Identifier getPinNumber() {
        return pinNumber;
    }

    public void setPinNumber(Identifier pinNumber) {
        this.pinNumber = pinNumber;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ModeSet modeSet = (ModeSet) o;
        return Objects.equals(pinNumber, modeSet.pinNumber) &&
                mode == modeSet.mode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pinNumber, mode);
    }
}
