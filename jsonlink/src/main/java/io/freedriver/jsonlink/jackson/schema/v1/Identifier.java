package io.freedriver.jsonlink.jackson.schema.v1;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.freedriver.jsonlink.jackson.PinNumberKeyDeserializer;
import io.freedriver.jsonlink.jackson.PinNumberSerializer;

import java.util.Objects;

@JsonSerialize(using = PinNumberSerializer.class)
@JsonDeserialize(keyUsing = PinNumberKeyDeserializer.class)
public class Identifier {
    private final int pin;

    public Identifier(int pin) {
        this.pin = pin;
    }

    public static Identifier of(int pin) {
        return new Identifier(pin);
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
        Identifier pinNumber = (Identifier) o;
        return pin == pinNumber.pin;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pin);
    }

    /*
     * Convenience methods
     */

    public DigitalWrite setDigital(DigitalState b) {
        return new DigitalWrite(this, b);
    }

    public DigitalWrite setDigital(boolean b) {
        return new DigitalWrite(this, b);
    }

    public AnalogRead getAnalog(float voltage, float resistance) {
        return new AnalogRead(this, voltage, resistance);
    }

    public ModeSet setMode(Mode mode) {
        return new ModeSet(this, mode);
    }
}
