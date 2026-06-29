package io.freedriver.autonomy.vedirect;

import java.util.function.Function;

import io.freedriver.autonomy.jpa.entity.VEDirectMessage;
import jakarta.persistence.metamodel.SingularAttribute;

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
