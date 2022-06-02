package io.freedriver.autonomy.deployments.common.cdi.provider;

// import org.infinispan.Cache;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;

import javax.enterprise.inject.Produces;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public abstract class AbstractCacheProvider {
    private final EmbeddedCacheManager embeddedCacheManager = new DefaultCacheManager(true);

    private final Map<String, Configuration> configurations = new HashMap<>();

    protected <V, K> Cache<K,V> createCache(String cacheName, Supplier<Configuration> configuration) {
        return createInfinispanCache(cacheName, configuration);
    }


    protected synchronized <V, K> org.infinispan.Cache<K,V> createInfinispanCache(String cacheName, Supplier<Configuration> configuration) {
        return embeddedCacheManager.cacheExists(cacheName)
                ? embeddedCacheManager.getCache(cacheName)
                : embeddedCacheManager.createCache(
                cacheName,
                configurations.computeIfAbsent(cacheName, (s) -> configuration.get()));
    }
}
