package io.freedriver.autonomy.deployments.joystick.cache;

import io.freedriver.autonomy.deployments.common.cdi.provider.AbstractCacheProvider;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.ConfigurationBuilder;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class JoystickCacheProvider extends AbstractCacheProvider {
    public static final String CONNECTOR_CACHE = "connector_cache";
    public static final String SENSOR_CACHE = "sensor_cache";

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
}
