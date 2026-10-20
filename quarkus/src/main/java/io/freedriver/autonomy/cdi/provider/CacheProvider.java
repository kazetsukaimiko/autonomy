package io.freedriver.autonomy.cdi.provider;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.freedriver.autonomy.cdi.qualifier.AttributeCache;
import io.freedriver.autonomy.cdi.qualifier.AutonomyCache;
import io.freedriver.autonomy.cdi.qualifier.ConnectorCache;
import io.freedriver.autonomy.cdi.qualifier.OneSecondCache;
import io.freedriver.autonomy.cdi.qualifier.SensorCache;
import io.freedriver.autonomy.cdi.qualifier.SpeechCache;
import io.freedriver.autonomy.service.PinCoordinate;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class CacheProvider {

    @Produces
    @OneSecondCache
    public <K, V> Map<K, V> oneSecondCache() {
        return new ConcurrentHashMap<>();
    }

    @Produces
    @AttributeCache
    public <K, V> Map<K, V> attributeCache() {
        return new ConcurrentHashMap<>();
    }

    // One map for the alias service and the MQTT publisher. Dependent scope would
    // give each injection point its own map, so a group switch would not publish.
    @Produces
    @ApplicationScoped
    @ConnectorCache
    public Map<PinCoordinate, Boolean> connectorCache() {
        return new ConcurrentHashMap<>();
    }

    @Produces
    @SensorCache
    public <K, V> Map<K, V> sensorCache() {
        return new ConcurrentHashMap<>();
    }

    @Produces
    @AutonomyCache
    public <K, V> Map<K, V> autonomyCache() {
        return new ConcurrentHashMap<>();
    }

    @Produces
    @SpeechCache
    public <K, V> Map<K, V> speechCache() {
        return new ConcurrentHashMap<>();
    }
}
