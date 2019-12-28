package io.freedriver.jsonlink.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.freedriver.jsonlink.jackson.schema.v1.Identifier;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class PinName {
    private Identifier pinNumber;
    private String pinName;

    public PinName() {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PinName pinName1 = (PinName) o;
        return pinNumber == pinName1.pinNumber &&
                Objects.equals(pinName, pinName1.pinName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pinNumber, pinName);
    }

    @Override
    public String toString() {
        return "PinName{" +
                "pinNumber=" + pinNumber +
                ", pinName='" + pinName + '\'' +
                '}';
    }

    @JsonIgnore
    public String getGroup() {
        return Optional.ofNullable(getPinName())
                .filter(s -> !s.isBlank())
                .map(s -> s.split("_")[0])
                .orElse(null);
    }

    @JsonIgnore
    public String getUnit() {
        return Optional.ofNullable(getPinName())
                .filter(s -> !s.isBlank())
                .map(s -> s.split("_")[1])
                .orElse(null);
    }

    public static PinName fromEntry(Map.Entry<Identifier, String> identifierStringEntry) {
        PinName pinName = new PinName();
        pinName.setPinName(identifierStringEntry.getValue());
        pinName.setPinNumber(identifierStringEntry.getKey());
        return pinName;
    }

    public boolean ofGroup(String group) {
        return Objects.equals(group, getGroup());
    }
}
