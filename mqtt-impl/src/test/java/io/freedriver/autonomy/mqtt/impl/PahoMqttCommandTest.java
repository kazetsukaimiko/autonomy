package io.freedriver.autonomy.mqtt.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.freedriver.autonomy.mqtt.ApplianceCommandSink;
import io.freedriver.autonomy.mqtt.ApplianceStateSource;
import io.freedriver.autonomy.mqtt.MqttLog;
import io.freedriver.autonomy.mqtt.MqttSettings;
import io.freedriver.mqtt.contract.ApplianceCommandMessage;
import io.freedriver.mqtt.contract.ApplianceJson;
import io.freedriver.mqtt.contract.ApplianceSchemas;
import io.freedriver.mqtt.contract.ApplianceStateMessage;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.LoggerFactory;

@Timeout(30)
class PahoMqttCommandTest {

    private static final String STORE_PASS = "changeit";
    private static final String USER = "autonomy";
    private static final String SECRET = "xK9mQ2vL7p";
    private static final UUID INSTANCE_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final String STATE_TOPIC = ApplianceSchemas.appliancesTopic(INSTANCE_ID);
    private static final String COMMAND_TOPIC = ApplianceSchemas.commandsTopic(INSTANCE_ID);

    private static Path certDir;
    private static Path keystore;
    private static Path pem;

    private ch.qos.logback.classic.Logger mqttLogger;
    private ListAppender<ILoggingEvent> appender;
    private Path passwordFile;
    private EmbeddedMqttBroker broker;
    private MqttClient subscriber;
    private MqttClient publisher;
    private PahoMqttConnector connector;
    private Thread worker;
    private boolean commandsOn;
    private int maxBytes = 4096;
    private ApplianceCommandSink sink = (name, on) -> false;
    private final List<Toggle> toggles = new CopyOnWriteArrayList<>();
    private final List<Captured> messages = new CopyOnWriteArrayList<>();

    @BeforeAll
    static void certificates() throws Exception {
        certDir = Files.createTempDirectory("mqtt-command-certs-");
        keystore = certDir.resolve("broker.p12");
        pem = certDir.resolve("broker.pem");
        keytool("-genkeypair", "-alias", "broker", "-keyalg", "RSA", "-keysize", "2048", "-validity", "2",
                "-storetype", "PKCS12", "-keystore", keystore.toString(),
                "-storepass", STORE_PASS, "-keypass", STORE_PASS,
                "-dname", "CN=127.0.0.1", "-ext", "SAN=dns:localhost,ip:127.0.0.1", "-noprompt");
        keytool("-exportcert", "-rfc", "-alias", "broker", "-keystore", keystore.toString(),
                "-storepass", STORE_PASS, "-file", pem.toString());
    }

    @AfterAll
    static void deleteCertificates() throws IOException {
        if (certDir == null) {
            return;
        }
        try (var files = Files.walk(certDir)) {
            files.sorted((left, right) -> right.compareTo(left)).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // Temp dir cleanup is best-effort.
                }
            });
        }
    }

    @BeforeEach
    void logs() {
        mqttLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(MqttLog.CATEGORY);
        mqttLogger.setLevel(Level.DEBUG);
        appender = new ListAppender<>();
        appender.start();
        mqttLogger.addAppender(appender);
        commandsOn = false;
        maxBytes = 4096;
        sink = (name, on) -> false;
        toggles.clear();
        messages.clear();
    }

    @AfterEach
    void stop() throws Exception {
        if (connector != null) {
            connector.close();
        }
        if (worker != null) {
            worker.join(5_000);
        }
        close(publisher);
        close(subscriber);
        if (broker != null) {
            broker.close();
        }
        if (mqttLogger != null && appender != null) {
            mqttLogger.detachAppender(appender);
        }
        for (String line : lines()) {
            assertFalse(line.contains(SECRET), line);
            assertFalse(line.contains("applianceName"), line);
        }
    }

    @Test
    void commandTopicIsTheExactCommandsTopic() {
        assertEquals("freedriver/v1/" + INSTANCE_ID + "/commands", PahoMqttConnector.commandTopic(INSTANCE_ID));
        assertEquals(COMMAND_TOPIC, ApplianceSchemas.COMMANDS_TOPIC_TEMPLATE.replace("{instanceId}", INSTANCE_ID.toString()));
        assertFalse(COMMAND_TOPIC.contains("+"));
        assertFalse(COMMAND_TOPIC.contains("#"));
        assertEquals(1, ApplianceSchemas.QOS);
        assertFalse(ApplianceSchemas.RETAIN);
    }

    @Test
    void goodCommandTogglesOnlyTheFridgeAndTheNextPeriodicPublishClearsTheId() throws Exception {
        String json = ApplianceJson.writeCommand(new ApplianceCommandMessage("cmd-fridge", "fridge", true));
        commandsOn = true;
        maxBytes = json.getBytes(StandardCharsets.UTF_8).length;
        MutableSource source = new MutableSource(Map.of("fridge", false, "lamp", true), 2);
        sink = (name, on) -> {
            if (!"fridge".equals(name)) {
                return false;
            }
            toggles.add(new Toggle(name, on));
            source.setStates(Map.of("fridge", on, "lamp", true));
            return true;
        };
        start(source);
        awaitSubscribed();
        publish(COMMAND_TOPIC, json, false);

        awaitState(message -> "cmd-fridge".equals(message.appliedCommandId()));
        ApplianceStateMessage applied = stateMessages().stream()
                .filter(message -> "cmd-fridge".equals(message.appliedCommandId()))
                .findFirst()
                .orElseThrow();
        assertEquals(Map.of("fridge", true, "lamp", true), applianceStates(applied));
        assertEquals(List.of(new Toggle("fridge", true)), toggles);

        await(() -> {
            boolean sawId = false;
            for (ApplianceStateMessage message : stateMessages()) {
                if ("cmd-fridge".equals(message.appliedCommandId())) {
                    sawId = true;
                } else if (sawId && message.appliedCommandId() == null) {
                    return true;
                }
            }
            return false;
        });
        awaitLog(line -> line.contains("command received topic=" + COMMAND_TOPIC) && line.contains("reason=received"));
        awaitLog(line -> line.contains("command applied topic=" + COMMAND_TOPIC) && line.contains("reason=applied"));
    }

    @Test
    void retainedCommandIsIgnoredAndALiveCommandStillApplies() throws Exception {
        commandsOn = true;
        acceptFridge();
        start(new MutableSource(Map.of("fridge", false), 1));
        awaitSubscribed();
        // Moquette 0.17 sends a QoS 1 retained delivery with the retain bit cleared
        // (createNotRetainedPublishMessage). Mosquitto sets the bit. Exercise the
        // same callback the broker uses, with the bit set.
        MqttMessage retained = new MqttMessage(command("retained-cmd", "fridge", true).getBytes(StandardCharsets.UTF_8));
        retained.setQos(ApplianceSchemas.QOS);
        retained.setRetained(true);
        connector.onCommand(settings(), COMMAND_TOPIC, retained);
        awaitLog(line -> line.contains("command rejected topic=" + COMMAND_TOPIC) && line.contains("reason=retained"));
        assertTrue(toggles.isEmpty());

        publish(COMMAND_TOPIC, command("live-cmd", "fridge", true), false);
        await(() -> toggles.equals(List.of(new Toggle("fridge", true))));
        awaitState(message -> "live-cmd".equals(message.appliedCommandId()));
    }

    @Test
    void malformedCommandsAreRejectedAndTheProcessKeepsRunning() throws Exception {
        commandsOn = true;
        acceptFridge();
        start(new MutableSource(Map.of("fridge", false), 1));
        awaitSubscribed();
        String[] bad = {
                "",
                "{",
                "{}",
                "{\"commandId\":\"\",\"applianceName\":\"fridge\",\"state\":true}",
                "{\"commandId\":\"x\",\"applianceName\":\"fridge\",\"state\":\"yes\"}",
                "{\"commandId\":\"x\",\"applianceName\":\"fridge\",\"state\":true,\"secret\":\"" + SECRET + "\"}",
        };
        for (String json : bad) {
            publish(COMMAND_TOPIC, json, false);
        }
        await(() -> lines().stream().filter(line -> line.contains("reason=malformed")).count() >= bad.length);
        assertTrue(toggles.isEmpty());
        publish(COMMAND_TOPIC, command("after-bad", "fridge", true), false);
        await(() -> toggles.equals(List.of(new Toggle("fridge", true))));
    }

    @Test
    void unknownApplianceDoesNotToggleAndFridgeStillCan() throws Exception {
        commandsOn = true;
        acceptFridge();
        start(new MutableSource(Map.of("fridge", false, "lamp", true), 2));
        awaitSubscribed();
        publish(COMMAND_TOPIC, command("unknown-cmd", "lamp", true), false);
        awaitLog(line -> line.contains("command rejected topic=" + COMMAND_TOPIC)
                && line.contains("reason=unknown appliance"));
        assertTrue(toggles.isEmpty());
        publish(COMMAND_TOPIC, command("fridge-cmd", "fridge", true), false);
        await(() -> toggles.equals(List.of(new Toggle("fridge", true))));
    }

    @Test
    void oversizedPayloadIsRejectedBeforeParse() throws Exception {
        commandsOn = true;
        maxBytes = 32;
        acceptFridge();
        start(new MutableSource(Map.of("fridge", false), 1));
        awaitSubscribed();
        String json = "{\"commandId\":\"cmd-big\",\"applianceName\":\"fridge\",\"state\":true,\"pad\":\""
                + SECRET + "\"}";
        assertTrue(json.getBytes(StandardCharsets.UTF_8).length > maxBytes);
        publish(COMMAND_TOPIC, json, false);
        awaitLog(line -> line.contains("command rejected topic=" + COMMAND_TOPIC)
                && line.contains("reason=payload too large"));
        Thread.sleep(300);
        assertTrue(lines().stream().noneMatch(line -> line.contains("reason=malformed")));
        assertTrue(toggles.isEmpty());
        awaitState(message -> message.appliedCommandId() == null);
    }

    @Test
    void commandsDisabledDoesNotSubscribe() throws Exception {
        commandsOn = false;
        acceptFridge();
        start(new MutableSource(Map.of("fridge", false), 1));
        awaitState(message -> message.appliedCommandId() == null);
        publish(COMMAND_TOPIC, command("ignored", "fridge", true), false);
        Thread.sleep(400);
        assertTrue(toggles.isEmpty());
        assertTrue(lines().stream().noneMatch(line -> line.contains("commands subscribed")));
        assertTrue(lines().stream().noneMatch(line -> line.contains("command received")));
    }

    @Test
    void backlogAppliesOnlyTheLatestCommandForTheFridge() throws Exception {
        commandsOn = true;
        MutableSource source = new MutableSource(Map.of("fridge", false), 1);
        sink = (name, on) -> {
            if (!"fridge".equals(name)) {
                return false;
            }
            toggles.add(new Toggle(name, on));
            source.setStates(Map.of("fridge", on));
            return true;
        };
        connector = new PahoMqttConnector(
                ReconnectPolicy.standard(), Duration.ofMillis(50), Duration.ofMinutes(1), 5);
        connector.pauseCommandApplies();
        openBroker();
        startConnector(source);
        awaitSubscribed();
        publish(COMMAND_TOPIC, command("old-cmd", "fridge", false), false);
        publish(COMMAND_TOPIC, command("new-cmd", "fridge", true), false);
        awaitLog(line -> line.contains("command rejected topic=" + COMMAND_TOPIC) && line.contains("reason=superseded"));
        assertTrue(toggles.isEmpty());
        connector.resumeCommandApplies();
        await(() -> toggles.equals(List.of(new Toggle("fridge", true))));
        awaitState(message -> "new-cmd".equals(message.appliedCommandId()));
        assertTrue(stateMessages().stream().noneMatch(message -> "old-cmd".equals(message.appliedCommandId())));
    }

    @Test
    void aCommandOnAnotherTopicIsIgnored() throws Exception {
        commandsOn = true;
        acceptFridge();
        start(new MutableSource(Map.of("fridge", false), 1));
        awaitSubscribed();
        publish("freedriver/v1/other/commands", command("other", "fridge", true), false);
        Thread.sleep(300);
        assertTrue(toggles.isEmpty());
        publish(COMMAND_TOPIC, command("exact", "fridge", true), false);
        await(() -> toggles.equals(List.of(new Toggle("fridge", true))));
    }

    private void acceptFridge() {
        sink = (name, on) -> {
            if (!"fridge".equals(name)) {
                return false;
            }
            toggles.add(new Toggle(name, on));
            return true;
        };
    }

    private void start(ApplianceStateSource source) throws Exception {
        openBroker();
        startConnector(source);
    }

    private void openBroker() throws Exception {
        broker = startBroker();
        publisher = client("pub");
        subscriber = client("sub");
        subscriber.setCallback(new StateCatcher());
        subscriber.subscribe(STATE_TOPIC, ApplianceSchemas.QOS);
        armPublisher();
    }

    private void startConnector(ApplianceStateSource source) {
        worker = new Thread(() -> connector.start(settings(), source, sink), "test-mqtt-commands");
        worker.setDaemon(true);
        worker.start();
    }

    private static Map<String, Boolean> applianceStates(ApplianceStateMessage message) {
        Map<String, Boolean> states = new java.util.LinkedHashMap<>();
        message.appliances().forEach(appliance -> states.put(appliance.applianceName(), appliance.state()));
        return states;
    }

    private void armPublisher() throws IOException {
        passwordFile = Files.createTempFile("mqtt-password-", ".pass");
        Files.writeString(passwordFile, SECRET + "\n");
        Files.setPosixFilePermissions(passwordFile, PosixFilePermissions.fromString("rw-------"));
        if (connector == null) {
            connector = new PahoMqttConnector(
                    ReconnectPolicy.standard(), Duration.ofMillis(40), Duration.ofMinutes(1), 5);
        }
    }

    private EmbeddedMqttBroker startBroker() throws Exception {
        EmbeddedMqttBroker started = new EmbeddedMqttBroker(freePort(), keystore, STORE_PASS, USER, SECRET);
        started.start();
        return started;
    }

    private MqttClient client(String prefix) throws Exception {
        MqttClient client = new MqttClient("ssl://127.0.0.1:" + broker.port(), prefix + "-" + UUID.randomUUID(),
                new MemoryPersistence());
        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(false);
        options.setCleanSession(true);
        options.setUserName(USER);
        options.setPassword(SECRET.toCharArray());
        options.setConnectionTimeout(5);
        options.setHttpsHostnameVerificationEnabled(true);
        options.setSocketFactory(MqttTrust.socketFactory(Optional.of(pem)));
        client.connect(options);
        return client;
    }

    private void publish(String topic, String json, boolean retained) throws Exception {
        MqttMessage message = new MqttMessage(json.getBytes(StandardCharsets.UTF_8));
        message.setQos(ApplianceSchemas.QOS);
        message.setRetained(retained);
        publisher.publish(topic, message);
    }

    private static String command(String commandId, String applianceName, boolean state) {
        return ApplianceJson.writeCommand(new ApplianceCommandMessage(commandId, applianceName, state));
    }

    private MqttSettings settings() {
        Path file = passwordFile;
        int limit = maxBytes;
        boolean commands = commandsOn;
        return new MqttSettings() {
            @Override
            public boolean enabled() {
                return true;
            }

            @Override
            public String host() {
                return "127.0.0.1";
            }

            @Override
            public int port() {
                return broker.port();
            }

            @Override
            public String username() {
                return USER;
            }

            @Override
            public Path passwordFile() {
                return file;
            }

            @Override
            public Optional<Path> caFile() {
                return Optional.of(pem);
            }

            @Override
            public String clientId() {
                return "autonomy-" + INSTANCE_ID;
            }

            @Override
            public UUID instanceId() {
                return INSTANCE_ID;
            }

            @Override
            public String instanceName() {
                return "Cabin";
            }

            @Override
            public Duration publishInterval() {
                return Duration.ofMillis(150);
            }

            @Override
            public Duration keepalive() {
                return Duration.ofSeconds(30);
            }

            @Override
            public Duration connectTimeout() {
                return Duration.ofSeconds(2);
            }

            @Override
            public boolean commandsEnabled() {
                return commands;
            }

            @Override
            public int commandMaxBytes() {
                return limit;
            }
        };
    }

    private void awaitSubscribed() {
        awaitLog(line -> line.contains("commands subscribed topic=" + COMMAND_TOPIC)
                && line.contains("qos=1")
                && line.contains("retain=false")
                && !line.contains("+")
                && !line.contains("#"));
    }

    private void awaitState(java.util.function.Predicate<ApplianceStateMessage> match) {
        await(() -> stateMessages().stream().anyMatch(match));
    }

    private List<ApplianceStateMessage> stateMessages() {
        List<ApplianceStateMessage> parsed = new ArrayList<>();
        for (Captured captured : messages) {
            if (!STATE_TOPIC.equals(captured.topic())) {
                continue;
            }
            try {
                parsed.add(ApplianceJson.readState(new String(captured.payload(), StandardCharsets.UTF_8)));
            } catch (RuntimeException ignored) {
                // A partial payload is not a state message yet.
            }
        }
        return parsed;
    }

    private void awaitLog(java.util.function.Predicate<String> match) {
        await(() -> lines().stream().anyMatch(match));
    }

    private void await(BooleanSupplier condition) {
        long end = System.nanoTime() + Duration.ofSeconds(8).toNanos();
        while (System.nanoTime() < end) {
            if (condition.getAsBoolean()) {
                return;
            }
            try {
                Thread.sleep(25);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail("interrupted");
            }
        }
        fail("timed out; mqtt log was:\n" + String.join("\n", lines()));
    }

    private List<String> lines() {
        return new ArrayList<>(appender.list).stream().map(ILoggingEvent::getFormattedMessage).toList();
    }

    private static void close(MqttClient client) {
        if (client == null) {
            return;
        }
        try {
            client.disconnectForcibly(0, 0, false);
            client.close();
        } catch (Exception ignored) {
            // The broker may already be gone.
        }
    }

    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static void keytool(String... args) throws Exception {
        Path tool = Path.of(System.getProperty("java.home"), "bin", "keytool");
        List<String> command = new ArrayList<>();
        command.add(tool.toString());
        command.addAll(List.of(args));
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (process.waitFor() != 0) {
            throw new IllegalStateException(output);
        }
    }

    private final class StateCatcher implements org.eclipse.paho.client.mqttv3.MqttCallback {
        @Override
        public void connectionLost(Throwable cause) {
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) {
            messages.add(new Captured(topic, message.getPayload()));
        }

        @Override
        public void deliveryComplete(org.eclipse.paho.client.mqttv3.IMqttDeliveryToken token) {
        }
    }

    private static final class MutableSource implements ApplianceStateSource {
        private final AtomicReference<Map<String, Boolean>> states;
        private final int mappedCount;

        private MutableSource(Map<String, Boolean> states, int mappedCount) {
            this.states = new AtomicReference<>(states);
            this.mappedCount = mappedCount;
        }

        private void setStates(Map<String, Boolean> next) {
            states.set(next);
        }

        @Override
        public Snapshot read() {
            return new Snapshot(states.get(), mappedCount);
        }
    }

    private record Captured(String topic, byte[] payload) {
    }

    private record Toggle(String name, boolean on) {
    }
}
