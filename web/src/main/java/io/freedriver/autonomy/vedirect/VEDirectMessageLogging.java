package io.freedriver.autonomy.vedirect;

import kaze.victron.VEDirectMessage;

import java.time.Duration;
import java.util.stream.Stream;

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
                            480-vdm.getPanelPower().divide(2.5).intValue()
                    )));
        }

        @Override
        public boolean validate(VEDirectMessage vdm) {
            return vdm != null
                    && vdm.getProductType() != null
                    && vdm.getProductType().getProductName() != null
                    && vdm.getSerialNumber() != null;
        }
    },
    ;


    public abstract String getFieldName(VEDirectMessage vdm);
    public abstract String getMessage(VEDirectMessage vdm);
    public abstract Duration getInterval(VEDirectMessage vdm);
    public abstract boolean validate(VEDirectMessage vdm);

    public static Stream<VEDirectMessageLogging> stream() {
        return Stream.of(values());
    }
}
