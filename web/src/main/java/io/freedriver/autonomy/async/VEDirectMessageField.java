package io.freedriver.autonomy.async;

import kaze.victron.VEDirectMessage;

import java.util.Objects;
import java.util.function.Function;

public enum VEDirectMessageField {
    RELAY_STATE("Relay state", VEDirectMessage::getRelayState),
    STATE_OF_OPERATION("State of operation", VEDirectMessage::getStateOfOperation),
    TRACKER_OPERATION("Tracker operation", VEDirectMessage::getTrackerOperation),
    ERROR_CODE("Error code", VEDirectMessage::getErrorCode),
    OFF_REASON("Off reason", VEDirectMessage::getOffReason),
    YIELD_YESTERDAY("Yesterday's yield", VEDirectMessage::getYieldYesterday)

    ;

    private final String fieldName;
    private final Function<VEDirectMessage, String> getter;

    <T> VEDirectMessageField(String fieldName,Function<VEDirectMessage, T> getter, Function<T, String> toStringFunction) {
        this.fieldName = fieldName;
        this.getter = t -> toStringFunction.apply(getter.apply(t));
    }

    <T> VEDirectMessageField(String fieldName, Function<VEDirectMessage, T> getter) {
        this(fieldName, getter, Objects::toString);
    }

    public String getFieldName() {
        return fieldName;
    }

    public Function<VEDirectMessage, String> getGetter() {
        return getter;
    }

    public String apply(VEDirectMessage veDirectMessage) {
        return getGetter().apply(veDirectMessage);
    }
}
