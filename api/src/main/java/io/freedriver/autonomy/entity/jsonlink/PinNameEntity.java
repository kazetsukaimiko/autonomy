package io.freedriver.autonomy.entity.jsonlink;

import io.freedriver.jsonlink.jackson.schema.v1.Identifier;
import io.freedriver.jsonlink.jackson.schema.v1.Mode;

import java.util.Objects;

public class PinNameEntity extends JsonLinkEntity<PinNameEntity> {
    private Identifier pinNumber;
    private String pinName;
    private Mode pinMode;

    public PinNameEntity() {
    }

    public Identifier getPinNumber() {
        return pinNumber;
    }

    public void setPinNumber(Identifier pinNumber) {
        this.pinNumber = pinNumber;
    }

    public String getPinName() {
        return pinName;
    }

    public void setPinName(String pinName) {
        this.pinName = pinName;
    }

    public Mode getPinMode() {
        return pinMode;
    }

    public void setPinMode(Mode pinMode) {
        this.pinMode = pinMode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        PinNameEntity that = (PinNameEntity) o;
        return Objects.equals(pinNumber, that.pinNumber) &&
                Objects.equals(pinName, that.pinName) &&
                pinMode == that.pinMode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), pinNumber, pinName, pinMode);
    }

    @Override
    public String toString() {
        return "PinNameEntity{" +
                "pinNumber=" + pinNumber +
                ", pinName='" + pinName + '\'' +
                ", pinMode=" + pinMode +
                '}';
    }
}
