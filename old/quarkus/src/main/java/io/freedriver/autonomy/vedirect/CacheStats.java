package io.freedriver.autonomy.vedirect;

import io.freedriver.autonomy.jpa.entity.VEDirectMessage;
import io.freedriver.autonomy.jpa.entity.VEDirectMessage_;

import javax.persistence.metamodel.SingularAttribute;
import java.util.function.Function;

public enum CacheStats implements CacheStat {
    PANEL_VOLTAGE(VEDirectMessage_.panelVoltage, VEDirectMessage::getPanelVoltage),
    ;

    private final SingularAttribute<VEDirectMessage, ?> attribute;
    private final Function<VEDirectMessage, ?> function;

    <T> CacheStats(SingularAttribute<VEDirectMessage, T> attribute, Function<VEDirectMessage, T> function) {
        this.attribute = attribute;
        this.function = function;
    }


    @Override
    public SingularAttribute<VEDirectMessage, ?> getAttribute() {
        return attribute;
    }

    @Override
    public Object getFromMessage(VEDirectMessage message) {
        return function.apply(message);
    }
}
