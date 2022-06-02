package io.freedriver.autonomy.deployments.common.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.moquette.broker.Server;
import io.moquette.broker.config.MemoryConfig;
import io.moquette.interception.InterceptHandler;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

import javax.enterprise.event.Observes;
import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MQTTBroker implements Closeable {

    private static final String INTERCEPT_ID = UUID.randomUUID().toString();
    private static final Logger LOGGER = Logger.getLogger(MQTTBroker.class.getName());
    private static final String USERNAME_PROPERTY = "autonomy.mqtt.username";
    private static final String PASSWORD_PROPERTY = "autonomy.mqtt.password";

    private final Server server;

    public MQTTBroker(Properties properties, InterceptHandler... interceptHandlers) throws IOException {
        server = new Server();
        MemoryConfig config = new MemoryConfig(properties);
        server.startServer(config);
        Arrays.stream(interceptHandlers)
                .forEach(server::addInterceptHandler);
    }


    @Override
    public void close() throws IOException {
        server.stopServer();
    }
}
