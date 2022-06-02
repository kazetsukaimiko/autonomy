package io.freedriver.autonomy.deployments.common.service;

import io.freedriver.jsonlink.config.ConnectorConfig;
import io.quarkus.runtime.StartupEvent;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@ApplicationScoped
public class ConfigurationService {
    private static final Logger LOGGER = Logger.getLogger(ConfigurationService.class);

    @Produces @Default
    public ConnectorConfig getConfiguration() throws IOException {
        return ConnectorConfig.load();
    }

    private Map<String, String> getConfigProperties() {
        return StreamSupport.stream(ConfigProvider.getConfig().getConfigSources().spliterator(), false)
                .map(ConfigSource::getProperties)
                .map(Map::entrySet)
                .flatMap(Set::stream)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b));
    }

    public Map<String, String> getConfigPropertiesStartingWith(String prefix) {
        return getConfigProperties()
                .entrySet()
                .stream()
                .filter(e -> e.getKey().startsWith(prefix))
                .map(e -> Map.entry(e.getKey().substring(prefix.length()), e.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b));
    }

    public void logAllPropertiesAtBoot(@Observes StartupEvent ev) {
        LOGGER.info("Available config properties:");
        getConfigProperties()
                .entrySet()
                .stream()
                .map(e -> e.getKey() + ":" + e.getValue())
                .forEach(LOGGER::debug);
    }


}
