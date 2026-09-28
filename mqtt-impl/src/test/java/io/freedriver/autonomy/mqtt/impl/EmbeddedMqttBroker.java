package io.freedriver.autonomy.mqtt.impl;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import io.moquette.BrokerConstants;
import io.moquette.broker.Server;
import io.moquette.broker.config.MemoryConfig;
import io.moquette.broker.security.IAuthenticator;
import io.moquette.broker.security.IAuthorizatorPolicy;
import io.moquette.broker.subscriptions.Topic;
import io.moquette.interception.InterceptHandler;

/**
 * Local MQTTS broker for connector tests. Plain TCP is left disabled.
 */
final class EmbeddedMqttBroker implements AutoCloseable {

    private final int port;
    private final Path keystore;
    private final String storePassword;
    private final String username;
    private final String password;
    private Server server;

    EmbeddedMqttBroker(int port, Path keystore, String storePassword, String username, String password) {
        this.port = port;
        this.keystore = keystore;
        this.storePassword = storePassword;
        this.username = username;
        this.password = password;
    }

    int port() {
        return port;
    }

    void start() throws Exception {
        stop();
        Path data = Files.createTempDirectory("moquette-");
        Properties properties = new Properties();
        properties.setProperty(BrokerConstants.HOST_PROPERTY_NAME, "127.0.0.1");
        properties.setProperty(BrokerConstants.PORT_PROPERTY_NAME, BrokerConstants.DISABLED_PORT_BIND);
        properties.setProperty(BrokerConstants.WEB_SOCKET_PORT_PROPERTY_NAME, BrokerConstants.DISABLED_PORT_BIND);
        properties.setProperty(BrokerConstants.WSS_PORT_PROPERTY_NAME, BrokerConstants.DISABLED_PORT_BIND);
        properties.setProperty(BrokerConstants.SSL_PORT_PROPERTY_NAME, Integer.toString(port));
        properties.setProperty(BrokerConstants.JKS_PATH_PROPERTY_NAME, keystore.toAbsolutePath().toString());
        properties.setProperty(BrokerConstants.KEY_STORE_TYPE, "PKCS12");
        properties.setProperty(BrokerConstants.KEY_STORE_PASSWORD_PROPERTY_NAME, storePassword);
        properties.setProperty(BrokerConstants.KEY_MANAGER_PASSWORD_PROPERTY_NAME, storePassword);
        properties.setProperty(BrokerConstants.SSL_PROVIDER, "JDK");
        properties.setProperty(BrokerConstants.NEED_CLIENT_AUTH, "false");
        properties.setProperty(BrokerConstants.ALLOW_ANONYMOUS_PROPERTY_NAME, "false");
        properties.setProperty(BrokerConstants.PERSISTENCE_ENABLED_PROPERTY_NAME, "false");
        properties.setProperty(BrokerConstants.DATA_PATH_PROPERTY_NAME, data.toString());
        properties.setProperty(BrokerConstants.PERSISTENT_STORE_PROPERTY_NAME, data.resolve("store.h2").toString());
        properties.setProperty("netty.so_reuseaddr", "true");
        properties.setProperty("immediate_buffer_flush", "true");
        Server started = new Server();
        started.startServer(new MemoryConfig(properties), List.<InterceptHandler>of(), null,
                new PasswordAuthenticator(username, password), new AllowAll());
        this.server = started;
    }

    void stop() {
        Server running = server;
        server = null;
        if (running != null) {
            running.stopServer();
        }
    }

    @Override
    public void close() {
        stop();
    }

    private record PasswordAuthenticator(String username, String password) implements IAuthenticator {
        @Override
        public boolean checkValid(String clientId, String user, byte[] secret) {
            if (secret == null) {
                return false;
            }
            return username.equals(user) && password.equals(new String(secret, java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    private static final class AllowAll implements IAuthorizatorPolicy {
        @Override
        public boolean canWrite(Topic topic, String user, String client) {
            return true;
        }

        @Override
        public boolean canRead(Topic topic, String user, String client) {
            return true;
        }
    }
}
