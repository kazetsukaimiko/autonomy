package io.freedriver.autonomy.async;

import kaze.math.potential.Potential;
import kaze.victron.VEDirectMessage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static kaze.math.Multiplier.BASE;

public enum VEDirectMessageChange {
    RELAY_STATE("Relay state", VEDirectMessage::getRelayState),
    STATE_OF_OPERATION("State of operation", VEDirectMessage::getStateOfOperation),
    TRACKER_OPERATION("Tracker operation", VEDirectMessage::getTrackerOperation),
    ERROR_CODE("Error code", VEDirectMessage::getErrorCode),
    OFF_REASON("Off reason", VEDirectMessage::getOffReason),
    YIELD_YESTERDAY("Yesterday's yield", VEDirectMessage::getYieldYesterday),
    BATTERY_VOLTAGE("Battery Voltage",
            (o, n) -> tenthOfaVolt(o.getMainVoltage()).compareTo(tenthOfaVolt(n.getMainVoltage())) != 0,
            (o, n) ->
                o == n ?
                    "Current voltage: " + n.getMainVoltage()
                :
                    "Main Voltage " + (o.getMainVoltage().lessThan(n.getMainVoltage()) ? "Rose":"Fell")
                    + " from " + o.getMainVoltage() + " to " + n.getMainVoltage()),
    PANEL_YIELD_CHANGE("Panel Power",
            (o, n) -> o != null && n != null && o.getPanelPower().subtract(o.getPanelPower()).greaterThan(BASE.watts(new BigDecimal("50"))),
            (o, n) -> o == n ? "Current Panel Power: " + n.getPanelPower() : "Panel Power Change: " + o.getPanelPower() + " -> " + n.getPanelPower())
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

    public static BigDecimal tenthOfaVolt(Potential potential) {
        return potential.getValue().divide(BASE.volts(new BigDecimal("0.1")).getValue(), RoundingMode.FLOOR);
    }
}
