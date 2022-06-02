package io.freedriver.autonomy.deployments.common.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.moquette.interception.InterceptHandler;
import io.moquette.interception.messages.*;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import java.io.IOException;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ApplicationScoped denotes that this class is instantiated once, and is global to the application.
 * The InterceptHandler interface is how we catch Moquette MQTT messages for processing/persistence.
 */
@ApplicationScoped
public class MQTTBrokerService implements InterceptHandler {

    private static final String INTERCEPT_ID = UUID.randomUUID().toString();
    private static final Logger LOGGER = Logger.getLogger(MQTTBrokerService.class.getName());
    private static final String PORT_PROPERTY = "autonomy.mqtt.tcp.port";
    private static final String WSPORT_PROPERTY = "autonomy.mqtt.websocket.port";
    private static final String USERNAME_PROPERTY = "autonomy.mqtt.username";
    private static final String PASSWORD_PROPERTY = "autonomy.mqtt.password";

    @ConfigProperty(name = PORT_PROPERTY)
    Optional<String> tcpPort;

    @ConfigProperty(name = WSPORT_PROPERTY)
    Optional<Integer> websocketPort;

    /*
    @ConfigProperty(name = USERNAME_PROPERTY)
    String mqttUsername;

    @ConfigProperty(name = PASSWORD_PROPERTY)
    String mqttPassword;
     */

    // TODO replace with injection or MessageConverter
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
            .enable(SerializationFeature.INDENT_OUTPUT);

    private MQTTBroker broker;

    /**
     * Starts the MQTT Server protocols with our MQTT service.
     */
    public void startup(@Observes StartupEvent event) throws IOException {
        if (tcpPort.isPresent()) {
            LOGGER.info("Starting MQTT Broker...");
            Properties properties = System.getProperties();
            properties.put("port", tcpPort);
            websocketPort.ifPresent(wsPort -> properties.put("websocket_port", wsPort));
            broker = new MQTTBroker(properties, this);
        }
    }

    /**
     * Shuts down the MQTT Server protocols with our MQTT service.
     */
    public void shutdown(@Observes ShutdownEvent event) throws IOException {
        LOGGER.info("Shutting down MQTT broker...");
        broker.close();
    }

    /**
     * Log all messages as JSON data to the console.
     */
    public static <T extends InterceptMessage> void log(T t) {
        try {
            LOGGER.info(MAPPER.writeValueAsString(t));
        } catch (JsonProcessingException e) {
            LOGGER.log(Level.WARNING, "Unable to log message:", e);
        }
    }

    /*
     * MOQUETTE - MQTT Interface methods for observing MQTT traffic.
     */

    @Override
    public String getID() {
        return INTERCEPT_ID;
    }

    /**
     * We want custom behavior on all message types.
     */
    @Override
    public Class<?>[] getInterceptedMessageTypes() {
        return ALL_MESSAGE_TYPES;
    }

    /**
     * When someone connects.
     */
    @Override
    public void onConnect(InterceptConnectMessage interceptConnectMessage) {
        log(interceptConnectMessage);
    }

    /**
     * When someone disconnects.
     */
    @Override
    public void onDisconnect(InterceptDisconnectMessage interceptDisconnectMessage) {
        log(interceptDisconnectMessage);
    }

    /**
     * When a connection is lost.
     */
    @Override
    public void onConnectionLost(InterceptConnectionLostMessage interceptConnectionLostMessage) {
        log(interceptConnectionLostMessage);
    }

    /**
     * When a topic is published.
     */
    @Override
    public void onPublish(InterceptPublishMessage interceptPublishMessage) {
        log(interceptPublishMessage);
    }

    /**
     * When someone subscribes to a topic.
     */
    @Override
    public void onSubscribe(InterceptSubscribeMessage interceptSubscribeMessage) {
        log(interceptSubscribeMessage);
    }

    /**
     * When someone unsubscribes from a topic
     */
    @Override
    public void onUnsubscribe(InterceptUnsubscribeMessage interceptUnsubscribeMessage) {
        log(interceptUnsubscribeMessage);
    }

    /**
     * When a message is acknowledged.
     */
    @Override
    public void onMessageAcknowledged(InterceptAcknowledgedMessage interceptAcknowledgedMessage) {
        log(interceptAcknowledgedMessage);
    }
}
