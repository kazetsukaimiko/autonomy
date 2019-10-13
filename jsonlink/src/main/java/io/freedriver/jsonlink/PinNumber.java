package io.freedriver.jsonlink;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.Objects;
import java.util.stream.Stream;

@JsonSerialize(using = PinNumberSerializer.class)
@JsonDeserialize(keyUsing = PinNumberKeyDeserializer.class)
public class PinNumber {
    private final int pin;

    public PinNumber(int pin) {
        this.pin = pin;
    }

    public static PinNumber of(int pin) {
        return new PinNumber(pin);
    }

    public int getPin() {
        return pin;
    }

    @Override
    public String toString() {
        return String.valueOf(pin);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PinNumber pinNumber = (PinNumber) o;
        return pin == pinNumber.pin;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pin);
    }

    public DigitalWrite setDigital(boolean b) {
        return new DigitalWrite(this, b);
    }

    public AnalogRead getAnalog(int resistance) {
        return new AnalogRead(this, resistance);
    }

    public ModeSet setMode(Mode mode) {
        return new ModeSet(this, mode);
    }
}
