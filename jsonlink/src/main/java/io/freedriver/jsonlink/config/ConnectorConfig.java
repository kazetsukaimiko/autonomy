package io.freedriver.jsonlink.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.freedriver.jsonlink.jackson.JsonLinkModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConnectorConfig {
    private static final Path CONFIG_PATH = Paths.get(System.getProperty("user.home"), ".config/jsonlink");
    private static final Path CONFIG_FILE_PATH = Paths.get(CONFIG_PATH.toAbsolutePath().toString(), "connectors.json");
    private static final Logger LOGGER = Logger.getLogger(ConnectorConfig.class.getName());
    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JsonLinkModule()).enable(SerializationFeature.INDENT_OUTPUT);
    private List<String> ignoreDevices = new ArrayList<>();

    public ConnectorConfig() {
    }

    public List<String> getIgnoreDevices() {
        return ignoreDevices != null ?
                ignoreDevices : new ArrayList<>();
    }

    public void setIgnoreDevices(List<String> ignoreDevices) {
        this.ignoreDevices = ignoreDevices != null ?
            ignoreDevices : new ArrayList<>();
    }

    public static ConnectorConfig load() {
        try {
            return loadOrCreate();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Couldn't load connector.config, defaulting to new ", e);
            return new ConnectorConfig();
        }
    }

    private static ConnectorConfig loadOrCreate() throws IOException {
        if (!Files.isDirectory(CONFIG_PATH)) {
            Files.createDirectories(CONFIG_PATH);
            return loadOrCreate();
        } else {
            if (Files.isRegularFile(CONFIG_FILE_PATH)) {
                return MAPPER.readValue(CONFIG_FILE_PATH.toFile(), ConnectorConfig.class);
            } else {
                MAPPER.writeValue(CONFIG_FILE_PATH.toFile(), new ConnectorConfig());
                return loadOrCreate();
            }
        }
    }

    public boolean doNotIgnore(String s) {
        return !getIgnoreDevices().contains(s);
    }
}
