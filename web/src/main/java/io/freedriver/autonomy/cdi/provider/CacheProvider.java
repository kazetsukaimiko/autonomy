package io.freedriver.autonomy.cdi.provider;

import io.freedriver.autonomy.Autonomy;
import io.freedriver.autonomy.cdi.qualifier.AttributeCache;
import io.freedriver.autonomy.cdi.qualifier.AutonomyCache;
import io.freedriver.autonomy.cdi.qualifier.ConnectorCache;
import io.freedriver.autonomy.cdi.qualifier.OneSecondCache;
import org.infinispan.cdi.ConfigureCache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class CacheProvider {
    public static final String ONE_SECOND_CACHE = "one_second_cache";
    public static final String ATTRIBUTE_CACHE = "attribute_cache";
    public static final String CONNECTOR_CACHE = "connector_cache";

    @ConfigureCache(ONE_SECOND_CACHE)
    @OneSecondCache
    @Produces
    public Configuration oneSecondCacheConfig() {
        return new ConfigurationBuilder()
                .locking()
                .lockAcquisitionTimeout(10, TimeUnit.SECONDS)
                .expiration()
                .lifespan(1, TimeUnit.SECONDS)
                .memory()
                .size(10000)
                .build();
    }

    @ConfigureCache(ATTRIBUTE_CACHE)
    @Produces
    @AttributeCache
    public Configuration attributeCacheConfig() {
        return new ConfigurationBuilder()
                .locking()
                .lockAcquisitionTimeout(10, TimeUnit.SECONDS)
                .memory()
                .size(100)
                .build();
    }

    @ConfigureCache(CONNECTOR_CACHE)
    @Produces
    @ConnectorCache
    public Configuration connectorCacheConfig() {
        return new ConfigurationBuilder()
                .locking()
                .lockAcquisitionTimeout(10, TimeUnit.SECONDS)
                .expiration()
                .lifespan(100, TimeUnit.SECONDS)
                .memory()
                .size(1000)
                .build();
    }

    @ConfigureCache(Autonomy.DEPLOYMENT)
    @Produces
    @AutonomyCache
    public Configuration autonomyCacheConfig() {
        return new ConfigurationBuilder()
                .locking()
                .lockAcquisitionTimeout(10, TimeUnit.SECONDS)
                .expiration()
                .lifespan(10, TimeUnit.SECONDS)
                .memory()
                .size(100)
                .build();
    }


}
