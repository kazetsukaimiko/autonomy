package io.freedriver.autonomy.mqtt;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * Loads the single {@link MqttConnector} implementation from the runtime classpath.
 */
public final class MqttConnectors {

    private MqttConnectors() {
    }

    public static MqttConnector load() {
        Iterator<MqttConnector> connectors = ServiceLoader.load(MqttConnector.class).iterator();
        if (!connectors.hasNext()) {
            throw new IllegalStateException("No MQTT connector implementation on the classpath");
        }
        MqttConnector connector = connectors.next();
        if (connectors.hasNext()) {
            throw new IllegalStateException("More than one MQTT connector implementation on the classpath");
        }
        return connector;
    }
}
