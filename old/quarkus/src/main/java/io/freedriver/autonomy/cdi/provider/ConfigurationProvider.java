package io.freedriver.autonomy.cdi.provider;

import io.freedriver.jsonlink.config.ConnectorConfig;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import java.io.IOException;

@ApplicationScoped
public class ConfigurationProvider {
    @Produces @Default
    public ConnectorConfig getConfiguration() throws IOException {
        return ConnectorConfig.load();
    }

}
