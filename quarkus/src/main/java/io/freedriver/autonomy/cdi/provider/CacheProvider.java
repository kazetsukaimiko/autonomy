package io.freedriver.autonomy.cdi.provider;

import io.freedriver.autonomy.Autonomy;
import io.freedriver.autonomy.cdi.qualifier.AttributeCache;
import io.freedriver.autonomy.cdi.qualifier.AutonomyCache;
import io.freedriver.autonomy.cdi.qualifier.ConnectorCache;
import io.freedriver.autonomy.cdi.qualifier.OneSecondCache;
import io.freedriver.autonomy.cdi.qualifier.SensorCache;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@ApplicationScoped
public class CacheProvider {
    public static final String ONE_SECOND_CACHE = "one_second_cache";
    public static final String ATTRIBUTE_CACHE = "attribute_cache";
    public static final String CONNECTOR_CACHE = "connector_cache";
    public static final String SENSOR_CACHE = "sensor_cache";

    private final EmbeddedCacheManager embeddedCacheManager = new DefaultCacheManager(true);
    private Map<String, Configuration> configurations = new HashMap<>();

    @Inject
    Instance<Configuration> allConfigurations;

    @Produces
    @OneSecondCache
    public <K, V> Cache<K, V> oneSecondCache() {
        return createCache(ONE_SECOND_CACHE, () -> new ConfigurationBuilder()
                .locking()
                .lockAcquisitionTimeout(10, TimeUnit.SECONDS)
                .expiration()
                .lifespan(1, TimeUnit.SECONDS)
                .memory()
                .maxCount(10000)
                .build());
    }

    @Produces
    @AttributeCache
    public <K, V> Cache<K, V> attributeCache() {
        return createCache(ATTRIBUTE_CACHE, () -> new ConfigurationBuilder()
                .locking()
                .lockAcquisitionTimeout(10, TimeUnit.SECONDS)
                .memory()
                .maxCount(100)
                .build());
    }

    @Produces
    @ConnectorCache
    public <K, V> Cache<K, V> connectorCache() {
        return createCache(CONNECTOR_CACHE, () -> new ConfigurationBuilder()
                .locking()
                .lockAcquisitionTimeout(10, TimeUnit.SECONDS)
                .expiration()
                .lifespan(100, TimeUnit.SECONDS)
                .memory()
                .maxCount(1000)
                .build());
    }


    @Produces
    @SensorCache
    public <K, V> Cache<K, V> sensorCache() {
        return createCache(SENSOR_CACHE, () -> new ConfigurationBuilder()
                .locking()
                .lockAcquisitionTimeout(10, TimeUnit.SECONDS)
                .expiration()
                .lifespan(100, TimeUnit.SECONDS)
                .memory()
                .maxCount(1000)
                .build());
    }


    @Produces
    @AutonomyCache
    public <K, V> Cache<K, V> autonomyCache() {
        return createCache(Autonomy.DEPLOYMENT, () -> new ConfigurationBuilder()
                .locking()
                .lockAcquisitionTimeout(10, TimeUnit.SECONDS)
                .expiration()
                .lifespan(10, TimeUnit.SECONDS)
                .memory()
                .maxCount(100)
                .build());
    }


    private synchronized <V, K> Cache<K,V> createCache(String cacheName, Supplier<Configuration> configuration) {
        return embeddedCacheManager.cacheExists(cacheName)
                ? embeddedCacheManager.getCache(cacheName)
                : embeddedCacheManager.createCache(
                cacheName,
                configurations.computeIfAbsent(cacheName, (s) -> configuration.get()));
    }
}
