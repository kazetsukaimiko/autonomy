package io.freedriver.autonomy.deployments.common.cdi.provider;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.ConfigurationBuilder;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class DeclarativeConfigCacheProvider extends AbstractCacheProvider {
    public static final String MAPPING_CACHE = "mapping_cache";

    @Produces
    @MappingCache
    public <K, V> Cache<K, V> connectorCache() {
        return createCache(MAPPING_CACHE, () -> new ConfigurationBuilder()
                .locking()
                .lockAcquisitionTimeout(10, TimeUnit.SECONDS)
                .expiration()
                .lifespan(10, TimeUnit.SECONDS)
                .memory()
                .maxCount(1000)
                .build());
    }

    @Produces
    @CacheConfig
    public <K, V> Cache<K, V> declarativeCache(InjectionPoint injectionPoint) {
        CacheConfig cacheConfig = injectionPoint.getAnnotated()
                .getAnnotation(CacheConfig.class);
        assert cacheConfig.name() != null && !cacheConfig.name().isEmpty();
        return createCache(cacheConfig.name(), () -> CacheConfig.ConfigConcerns.applyAll(new ConfigurationBuilder(), cacheConfig)
                .build());
    }


}
