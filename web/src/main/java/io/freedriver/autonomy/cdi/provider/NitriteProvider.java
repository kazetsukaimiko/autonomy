package io.freedriver.autonomy.cdi.provider;


import io.freedriver.autonomy.cdi.qualifier.NitriteDatabase;
import io.freedriver.autonomy.cdi.qualifier.NitriteDatabaseLiteral;
import io.freedriver.autonomy.config.Configuration;
import org.dizitart.no2.Nitrite;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@ApplicationScoped
public class NitriteProvider {
    private final Map<Class<?>, Nitrite> cache = new ConcurrentHashMap<>(new HashMap<>());

    @Inject
    private Configuration configuration;

    @Produces
    @NitriteDatabase
    public Nitrite getNitrite(InjectionPoint injectionPoint) {
        NitriteDatabase nitriteDatabase = injectionPoint.getQualifiers()
                .stream()
                .filter(NitriteDatabase.class::isInstance)
                .map(NitriteDatabase.class::cast)
                .findFirst()
                .orElseGet(NitriteDatabaseLiteral::new);
        return getOrBuild(nitriteDatabase, () -> Nitrite.builder()
                .compressed()
                .filePath(ConfigurationProvider.inConfigDirectory(getEntityDatabase(nitriteDatabase.value()) + ".db")
                .toAbsolutePath().toString())
                .openOrCreate("nitrite", "nitrite")); // TODO
    }

    public static final String getEntityDatabase(Class<?> klazz) {
        return klazz == null || klazz == Object.class ?
                "Default" : klazz.getSimpleName();
    }

    public synchronized Nitrite getOrBuild(NitriteDatabase nitriteDatabase, Supplier<Nitrite> supplier) {
        if (!cache.containsKey(nitriteDatabase.value())) {
            cache.put(nitriteDatabase.value(), supplier.get());
        }
        return cache.get(nitriteDatabase.value());
    }

}
