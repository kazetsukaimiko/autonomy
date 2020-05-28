package io.freedriver.autonomy.cdi.provider;

import io.freedriver.autonomy.cdi.qualifier.OneSecondCache;
import org.infinispan.cdi.ConfigureCache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;

import javax.enterprise.inject.Produces;
import java.util.concurrent.TimeUnit;

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
}
