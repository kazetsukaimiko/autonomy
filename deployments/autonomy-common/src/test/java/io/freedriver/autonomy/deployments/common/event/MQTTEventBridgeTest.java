package io.freedriver.autonomy.deployments.common.event;

import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
public class MQTTEventBridgeTest {
    public static final String MQTT_PORT_PROPERTY = "event.bridge.mqtt.test.port";

    private static MQTTBroker broker;

    @Inject
    MQTTBrokerService brokerService;

    @Inject
    MQTTEventBridge eventBridge;

    @Inject
    TestEventService eventService;


    @Test
    public void testReaction() throws IOException, InterruptedException {
        assertTrue(eventService.getReceived().isEmpty());
        TestEvent testEvent = new TestEvent();


        Thread.sleep(1000);
        eventBridge.sendObject(testEvent);
        assertFalse(eventService.getReceived().isEmpty());
        assertTrue(eventService.getReceived().contains(testEvent));
    }




}
