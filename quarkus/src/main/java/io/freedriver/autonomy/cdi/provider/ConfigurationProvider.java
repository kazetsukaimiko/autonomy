package io.freedriver.autonomy.cdi.provider;

import java.io.IOException;

import io.freedriver.jsonlink.config.ConnectorConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class ConfigurationProvider {
    @Produces @Default
    public ConnectorConfig getConfiguration() throws IOException {
        return ConnectorConfig.load();
    }

}
