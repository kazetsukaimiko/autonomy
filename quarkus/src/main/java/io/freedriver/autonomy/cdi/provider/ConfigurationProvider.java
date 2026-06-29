package io.freedriver.autonomy.cdi.provider;

import java.io.IOException;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;

import io.freedriver.jsonlink.config.ConnectorConfig;

@ApplicationScoped
public class ConfigurationProvider {
    @Produces @Default
    public ConnectorConfig getConfiguration() throws IOException {
        return ConnectorConfig.load();
    }

}
