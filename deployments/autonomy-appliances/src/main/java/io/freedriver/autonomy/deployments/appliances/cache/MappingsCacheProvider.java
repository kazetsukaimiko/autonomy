package io.freedriver.autonomy.deployments.joystick.cache;

import io.freedriver.autonomy.deployments.common.cdi.provider.AbstractCacheProvider;
import io.freedriver.autonomy.deployments.common.cdi.provider.MappingCache;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.ConfigurationBuilder;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class MappingsCacheProvider extends AbstractCacheProvider {

}
