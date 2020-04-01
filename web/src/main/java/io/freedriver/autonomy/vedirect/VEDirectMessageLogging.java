package io.freedriver.autonomy.vedirect;

import kaze.math.measurement.units.Potential;
import kaze.victron.VEDirectMessage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.function.Function;
import java.util.stream.Stream;

import static kaze.math.Multiplier.BASE;

public enum VEDirectMessageLogging {
    PV_POWER(vdm -> vdm.getSerialNumber() + " Panel power", vdm -> vdm.getPanelPower().toString()),
    ;

    private final Function<VEDirectMessage, String> fieldName;
    private final Function<VEDirectMessage, String> message;
    private final Duration interval;

    VEDirectMessageLogging(Function<VEDirectMessage, String> fieldName, Function<VEDirectMessage, String> message, Duration interval) {
        this.fieldName = fieldName;
        this.message = message;
        this.interval = interval;
    }

    VEDirectMessageLogging(Function<VEDirectMessage, String> fieldName, Function<VEDirectMessage, String> message) {
        this(fieldName, message, Duration.ofMinutes(5));
    }


    public String getFieldName(VEDirectMessage veDirectMessage) {
        return fieldName.apply(veDirectMessage);
    }

    public Function<VEDirectMessage, String> getFieldName() {
        return fieldName;
    }

    public String getMessage(VEDirectMessage veDirectMessage) {
        return message.apply(veDirectMessage);
    }

    public Function<VEDirectMessage, String> getMessage() {
        return message;
    }

    public Duration getInterval() {
        return interval;
    }

    public static BigDecimal tenthOfaVolt(Potential potential) {
        return potential.getValue().divide(BASE.volts(new BigDecimal("10")).getValue(), RoundingMode.FLOOR);
    }

    public static Stream<VEDirectMessageLogging> stream() {
        return Stream.of(values());
    }
}
