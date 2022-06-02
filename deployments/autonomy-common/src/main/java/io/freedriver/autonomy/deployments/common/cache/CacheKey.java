package io.freedriver.autonomy.deployments.common.cache;

import java.util.Objects;

public class CacheKey<E, T> {
    private final E base;
    private final Class<T> klazz;

    public CacheKey(E base, Class<T> klazz) {
        this.base = base;
        this.klazz = klazz;
    }

    public E getBase() {
        return base;
    }

    public Class<T> getKlazz() {
        return klazz;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CacheKey<?, ?> cacheKey = (CacheKey<?, ?>) o;
        return Objects.equals(base, cacheKey.base) &&
                Objects.equals(klazz, cacheKey.klazz);
    }

    @Override
    public int hashCode() {
        return Objects.hash(base, klazz);
    }
}
