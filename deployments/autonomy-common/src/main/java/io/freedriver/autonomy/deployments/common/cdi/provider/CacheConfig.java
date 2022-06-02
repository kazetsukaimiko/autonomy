package io.freedriver.autonomy.deployments.common.cdi.provider;

import org.infinispan.configuration.cache.ConfigurationChildBuilder;

import javax.enterprise.util.Nonbinding;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheConfig {
    @Nonbinding
    String name() default "";
    @Nonbinding
    int lockAcquisitionTimeout() default 10;
    @Nonbinding
    int lifespan() default 10;
    @Nonbinding
    TimeUnit timeUnit() default TimeUnit.SECONDS;
    @Nonbinding
    int maxCount() default 1000;
    @Nonbinding
    CacheType type() default CacheType.Memory;

    @FunctionalInterface
    interface ConfigConcern extends BiFunction<ConfigurationChildBuilder, CacheConfig, ConfigurationChildBuilder> {
    }

    enum CacheType {
        Memory
        ;
    }

    enum ConfigConcerns implements ConfigConcern {
        LOCKING {
            @Override
            public ConfigurationChildBuilder apply(ConfigurationChildBuilder cb, CacheConfig cacheConfig) {
                return cb
                        .locking()
                        .lockAcquisitionTimeout(cacheConfig.lockAcquisitionTimeout(), cacheConfig.timeUnit());
            }
        },
        EXPIRATION {
            @Override
            public ConfigurationChildBuilder apply(ConfigurationChildBuilder cb, CacheConfig cacheConfig) {
                return cb
                        .expiration()
                        .lifespan(cacheConfig.lifespan(), cacheConfig.timeUnit());
            }
        },
        TYPE {
            @Override
            public ConfigurationChildBuilder apply(ConfigurationChildBuilder cb, CacheConfig cacheConfig) {
                switch (cacheConfig.type()) {
                    default: // Default to Memory.
                        return MEMORY_TYPE.apply(cb, cacheConfig);
                }
            }
        },
        MEMORY_TYPE {
            @Override
            public ConfigurationChildBuilder apply(ConfigurationChildBuilder cb, CacheConfig cacheConfig) {
                return cb
                        .memory()
                        .maxCount(cacheConfig.maxCount());
            }
        };

        public static ConfigurationChildBuilder applyAll(ConfigurationChildBuilder cb, CacheConfig cacheConfig) {
            for (ConfigConcern concern : values()) {
                cb = concern.apply(cb, cacheConfig);
            }
            return cb;
        }
    }

}
