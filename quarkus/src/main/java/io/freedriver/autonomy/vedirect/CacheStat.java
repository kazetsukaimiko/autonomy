package io.freedriver.autonomy.vedirect;

import java.util.function.Function;
import javax.persistence.metamodel.SingularAttribute;

import io.freedriver.autonomy.jpa.entity.VEDirectMessage;

public interface CacheStat<T> {
    SingularAttribute<VEDirectMessage, T> getAttribute();
    T getFromMessage(VEDirectMessage message);

    static <T> CacheStat<T> of(final SingularAttribute<VEDirectMessage, T> attribute,
                               final Function<VEDirectMessage, T> function) {
        return new CacheStat<>() {
            @Override
            public SingularAttribute<VEDirectMessage, T> getAttribute() {
                return attribute;
            }

            @Override
            public T getFromMessage(VEDirectMessage message) {
                return function.apply(message);
            }
        };
    }
}
