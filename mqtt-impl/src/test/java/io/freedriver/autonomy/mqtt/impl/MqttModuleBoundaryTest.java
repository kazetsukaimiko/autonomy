package io.freedriver.autonomy.mqtt.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import io.freedriver.autonomy.mqtt.MqttConnectors;
import org.junit.jupiter.api.Test;

class MqttModuleBoundaryTest {

    @Test
    void serviceLoaderLoadsThePahoConnector() {
        assertInstanceOf(PahoMqttConnector.class, MqttConnectors.load());
    }

    @Test
    void pahoAndTheWireContractStayOutOfTheApiAndTheService() throws IOException {
        Path root = Path.of("..").toAbsolutePath().normalize();
        assertAbsent(root.resolve("mqtt-api/src/main/java"), "org.eclipse.paho");
        assertAbsent(root.resolve("mqtt-api/src/main/java"), "io.freedriver.mqtt.contract");
        assertAbsent(root.resolve("quarkus/src/main/java"), "org.eclipse.paho");
        assertAbsent(root.resolve("quarkus/src/main/java"), "io.freedriver.autonomy.mqtt.impl");
    }

    private static void assertAbsent(Path sourceRoot, String token) throws IOException {
        try (var files = Files.walk(sourceRoot)) {
            files.filter(path -> path.toString().endsWith(".java")).forEach(path -> {
                try {
                    String text = Files.readString(path);
                    assertFalse(text.contains(token), path + " contains " + token);
                } catch (IOException e) {
                    throw new IllegalStateException(path.toString(), e);
                }
            });
        }
    }
}
