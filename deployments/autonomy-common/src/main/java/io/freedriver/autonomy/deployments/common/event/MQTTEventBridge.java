package io.freedriver.autonomy.deployments.common.event;

import io.freedriver.autonomy.deployments.common.util.JacksonMessaging;
import io.freedriver.autonomy.deployments.common.util.MessageConverter;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

@ApplicationScoped
public class MQTTEventBridge extends EventBridge implements MqttCallback {
    private static final Logger LOGGER = Logger.getLogger(MQTTEventBridge.class);
    private static final String TOPIC = MQTTEventBridge.class.getSimpleName();
    public static final String MQTT_SERVER_PROPERTY = "event.bridge.mqtt.server";

    // TODO : Interchangeable?
    private static final MessageConverter converter = new JacksonMessaging();

    @Inject
    ManagedExecutor pool;

    @ConfigProperty(name = MQTT_SERVER_PROPERTY)
    Optional<String> mqttServer;

    private MqttClient mqttClient;

    public void connect(@Observes StartupEvent startupEvent) throws MqttException {
        pool.submit(() -> {
        if (mqttServer.isPresent()) {
            boolean useSSL = mqttServer.get().startsWith("ssl://");
            LOGGER.info("MQTT Server connection info present");
            mqttClient = new MqttClient(
                    mqttServer.get(), // serverURI in format:
                    // "protocol://name:port"
                    MqttClient.generateClientId(), // ClientId
                    new MemoryPersistence()); // Persistence

            MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();

            // mqttConnectOptions.setUserName("<your_username>");
            // mqttConnectOptions.setPassword("<your_password>".toCharArray());

            LOGGER.infof("Setting up connection via %s", useSSL ? "ssl" : "tcp");
            // using the default socket factory
            mqttConnectOptions.setSocketFactory(
                    useSSL
                        ? SSLSocketFactory.getDefault()
                        : SocketFactory.getDefault());
            while (true) {
                try {
                    mqttClient.connect(mqttConnectOptions);
                    mqttClient.subscribe(TOPIC);
                    break;
                } catch (MqttException e) {
                    LOGGER.warn("Failed to connect to MQTT Server: ", e);
                    try {
                        await(Duration.ofSeconds(1));
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            mqttClient.setCallback(this);

            LOGGER.info("MqttClient Connected!");
            return true;
        }
        LOGGER.info("MqttClient not Connected!");
        return false;
        });
    }

    @Override
    // Called when the client lost the connection to the broker
    public void connectionLost(Throwable cause) {
        LOGGER.warn("MqttClient lost connection ", cause);
        while (true) {
            try {
                mqttClient.reconnect();
                break;
            } catch (MqttException e) {
                LOGGER.warn("Failed to reconnect to MQTT Server: ", e);
                try {
                    await(Duration.ofSeconds(1));
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        LOGGER.infof("Payload arrived for topic %s. Length; %d ", topic, message.getPayload().length);
        LOGGER.debugf(Arrays.toString(message.getPayload()));

        try {
            ReceivedItem<?> item = new ReceivedItem<>(converter.fromMessage(message.getPayload()));
            // TODO: Set expiry.
            bridgeToCDI(item);
        } catch (IOException e) {
            LOGGER.warn("Couldn't convert message payload. ", e);
        }
    }

    @Override
    // Called when an outgoing publish is complete
    public void deliveryComplete(IMqttDeliveryToken token) {
        System.out.println("delivery complete " + token);
    }

    @Override
    protected boolean supportsObject(Object o) {
        return mqttServer.isPresent() && mqttClient != null;
    }

    @Override
    protected void sendObject(Object o) throws IOException, InterruptedException {
        if (mqttServer.isPresent() && mqttClient.isConnected()) {
            try {
                mqttClient.publish(TOPIC, converter.toMessage(o), 2, false);
            } catch (MqttException e) {
                throw new IOException("Unable to publish message to MQTT Topic: ", e);
            }
        }
    }

    private void await(Duration duration) throws InterruptedException {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        }
    }

    @Override
    public void close() throws IOException {
        if (mqttServer.isPresent() && mqttClient.isConnected()) {
            try {
                mqttClient.disconnectForcibly();
            } catch (MqttException e) {
                throw new IOException("Couldn't disconnect forcibly", e);
            }
        }
    }
}
