package io.freedriver.jsonlink.jackson.schema.v1;

import java.util.Objects;

public class DigitalWrite {
    private Identifier pinNumber;
    private DigitalState operation;

    public DigitalWrite() {
    }

    public DigitalWrite(Identifier pinNumber, DigitalState operation) {
        this.pinNumber = pinNumber;
        this.operation = operation;
    }

    public DigitalWrite(Identifier pinNumber, boolean operation) {
        this(pinNumber, DigitalState.fromBoolean(operation));
    }

    public Identifier getPinNumber() {
        return pinNumber;
    }

    public void setPinNumber(Identifier pinNumber) {
        this.pinNumber = pinNumber;
    }

    public DigitalState getOperation() {
        return operation;
    }

    public void setOperation(DigitalState operation) {
        this.operation = operation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DigitalWrite pinWrite = (DigitalWrite) o;
        return operation == pinWrite.operation &&
                Objects.equals(pinNumber, pinWrite.pinNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pinNumber, operation);
    }
}
