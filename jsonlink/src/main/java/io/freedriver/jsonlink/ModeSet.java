package io.freedriver.jsonlink;

import java.util.Objects;

public class ModeSet {
    private PinNumber pinNumber;
    private Mode mode;

    public ModeSet() {
    }

    public ModeSet(PinNumber pinNumber, Mode mode) {
        this.pinNumber = pinNumber;
        this.mode = mode;
    }

    public PinNumber getPinNumber() {
        return pinNumber;
    }

    public void setPinNumber(PinNumber pinNumber) {
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
