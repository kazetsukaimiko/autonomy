package io.freedriver.autonomy.vedirect;

import kaze.math.measurement.units.Power;
import kaze.victron.VEDirectMessage;

import java.time.Duration;
import java.util.stream.Stream;

import static kaze.math.Multiplier.BASE;

public enum VEDirectMessageLogging {
    PV_POWER {
        @Override
        public String getFieldName(VEDirectMessage vdm) {
            return "Panel power for "+vdm.getProductType().getProductName() +" ("+vdm.getSerialNumber()+")";
        }

        @Override
        public String getMessage(VEDirectMessage vdm) {
            return vdm.getPanelPower().toString();
        }

        @Override
        public Duration getInterval(VEDirectMessage vdm) {
            return Duration.ofSeconds(
                    Math.max(2, Math.min(
                            300,
                            480-vdm.getPanelPower().divide(Power.of(2.5, BASE)).intValue()
                    )));
        }
    },
    ;


    public abstract String getFieldName(VEDirectMessage vdm);
    public abstract String getMessage(VEDirectMessage vdm);
    public abstract Duration getInterval(VEDirectMessage vdm);

    public static Stream<VEDirectMessageLogging> stream() {
        return Stream.of(values());
    }
}
