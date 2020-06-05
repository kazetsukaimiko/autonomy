package io.freedriver.autonomy.cdi.provider;

import io.freedriver.autonomy.Autonomy;
import io.freedriver.autonomy.cdi.qualifier.AttributeCache;
import io.freedriver.autonomy.cdi.qualifier.AutonomyCache;
import io.freedriver.autonomy.cdi.qualifier.OneSecondCache;
import org.infinispan.cdi.ConfigureCache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class CacheProvider {
    @ConfigureCache("onesecondcache")
    @OneSecondCache
    @Produces
    public Configuration oneSecondCacheConfig() {
        return new ConfigurationBuilder()
                .expiration()
                .lifespan(1, TimeUnit.SECONDS)
                .memory()
                .size(100)
                .build();
    }

    @ConfigureCache("attribute-cache")
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

    @ConfigureCache(Autonomy.DEPLOYMENT)
    @Produces
    @AutonomyCache
    public Configuration autonomyCacheConfig() {
        return new ConfigurationBuilder()
                .expiration()
                .lifespan(10, TimeUnit.SECONDS)
                .memory()
                .size(100)
                .build();
    }

}
