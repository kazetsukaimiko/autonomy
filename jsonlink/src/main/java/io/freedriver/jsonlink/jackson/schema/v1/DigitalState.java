package io.freedriver.jsonlink.jackson.schema.v1;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum DigitalState {
    HIGH(false),
    LOW(true);

    private final boolean value;

    DigitalState(boolean value) {
        this.value = value;
    }

    @JsonValue
    public boolean getValue() {
        return value;
    }

    @JsonCreator
    public static DigitalState fromBoolean(boolean modeValue) {
        return modeValue ? LOW : HIGH;
    }

    @Override
    public String toString() {
        return name();
    }
}
