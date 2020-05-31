package io.freedriver.autonomy.vedirect;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CacheStatMap {
    private final Map<CacheStat<?>, ?> map = new ConcurrentHashMap<>();

}
