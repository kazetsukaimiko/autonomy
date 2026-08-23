package io.freedriver.autonomy.vedirect;

import java.util.function.Function;

import io.freedriver.autonomy.jpa.entity.VEDirectMessage;

public enum CacheStats implements CacheStat {
    PANEL_VOLTAGE(VEDirectMessage::panelVoltage),
    ;

    private final Function<VEDirectMessage, ?> function;

    <T> CacheStats(Function<VEDirectMessage, T> function) {
        this.function = function;
    }

    @Override
    public Object getFromMessage(VEDirectMessage message) {
        return function.apply(message);
    }
}
