package io.freedriver.autonomy.async;

import kaze.math.power.Power;
import kaze.math.power.PowerUnit;
import kaze.victron.VEDirectMessage;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum VEDirectMessageChange {
    RELAY_STATE("Relay state", VEDirectMessage::getRelayState),
    STATE_OF_OPERATION("State of operation", VEDirectMessage::getStateOfOperation),
    TRACKER_OPERATION("Tracker operation", VEDirectMessage::getTrackerOperation),
    ERROR_CODE("Error code", VEDirectMessage::getErrorCode),
    OFF_REASON("Off reason", VEDirectMessage::getOffReason),
    YIELD_YESTERDAY("Yesterday's yield", VEDirectMessage::getYieldYesterday),
    PANEL_YIELD_CHANGE("Panel Yield Change",
            (o, n) -> o != null && n != null && o.getPanelPower().subtract(o.getPanelPower()).greaterThan(Power.of(new BigDecimal("50"), PowerUnit.WATTS)),
            (o, n) -> { return null; })
    ;

    private final String fieldName;
    private final BiPredicate<VEDirectMessage, VEDirectMessage> onChangePredicate;
    private final BiFunction<VEDirectMessage, VEDirectMessage, String> onChangeFunction;

    VEDirectMessageChange(
            String fieldName,
            BiPredicate<VEDirectMessage, VEDirectMessage> onChangePredicate,
            BiFunction<VEDirectMessage, VEDirectMessage, String> onChangeFunction) {
        this.fieldName = fieldName;
        this.onChangePredicate = onChangePredicate;
        this.onChangeFunction = onChangeFunction;
    }

    <T> VEDirectMessageChange(String fieldName, Function<VEDirectMessage, T> getter) {
        this(
                fieldName,
                (o, n) -> !Objects.equals(getter.apply(o), getter.apply(n)),
                (o, n) -> o == n ?
                        fieldName + " initial value: " + getter.apply(n)
                :
                        fieldName + " changed: "
                        + getter.apply(o) + " -> " + getter.apply(n)
        );
    }

    public String getFieldName() {
        return fieldName;
    }

    public BiPredicate<VEDirectMessage, VEDirectMessage> getOnChangePredicate() {
        return onChangePredicate;
    }

    public BiFunction<VEDirectMessage, VEDirectMessage, String> getOnChangeFunction() {
        return onChangeFunction;
    }

    public static String allValues(VEDirectMessage message) {
        return Stream.of(values())
                .map(field -> field.getOnChangeFunction().apply(message, message))
                .collect(Collectors.joining("\n"));
    }

    public void test(VEDirectMessage oldMessage, VEDirectMessage newMessage, Consumer<String> messageConsumer) {
        if (onChangePredicate.test(oldMessage, newMessage)) {
            messageConsumer.accept(onChangeFunction.apply(oldMessage, newMessage));
        }
    }
}
