package io.freedriver.autonomy.cdi.provider;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import io.freedriver.autonomy.config.ConfigFileTrust;
import io.freedriver.autonomy.config.ConnectionsDirectoryLoader;
import io.freedriver.jsonlink.config.ConnectorConfig;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class ConfigurationProvider {
    @Inject
    @ConfigProperty(name = "autonomy.connections.dir")
    String connectionsDir;

    private ConnectorConfig configuration;

    @PostConstruct
    void loadAtStartup() {
        Path dir = Path.of(connectionsDir);
        if (ConnectionsDirectoryLoader.directoryPresent(dir)) {
            try {
                configuration = ConnectionsDirectoryLoader.load(dir, ConfigFileTrust.processUid()).config();
            } catch (IOException e) {
                configuration = new ConnectorConfig(List.of());
            }
            return;
        }
        configuration = ConnectorConfig.load();
    }

    @Produces @Default
    public ConnectorConfig getConfiguration() {
        return configuration;
    }
}
