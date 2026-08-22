package io.freedriver.autonomy.vedirect;

import java.util.function.Function;

import io.freedriver.autonomy.jpa.entity.VEDirectMessage;

public interface CacheStat<T> {
    T getFromMessage(VEDirectMessage message);

    static <T> CacheStat<T> of(final Function<VEDirectMessage, T> function) {
        return function::apply;
    }
}
