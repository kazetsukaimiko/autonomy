package io.freedriver.autonomy.cache;

import lombok.Builder;

@Builder(toBuilder = true)
public record CacheKey<E, T>(E base, Class<T> klazz) {
}
