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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.JsonNode;
import io.freedriver.autonomy.mqtt.ApplianceStateSource;
import io.freedriver.autonomy.mqtt.MqttLog;
import io.freedriver.autonomy.mqtt.MqttSettings;
import io.freedriver.mqtt.contract.ApplianceJson;
import io.freedriver.mqtt.contract.ApplianceSchemas;
import io.freedriver.mqtt.contract.ApplianceStateMessage;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
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

class PahoMqttConnectorTest {

    private static final String STORE_PASS = "changeit";
    private static final String USER = "autonomy";
    private static final String SECRET = "xK9mQ2vL7p";
    private static final UUID INSTANCE_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final String TOPIC = ApplianceSchemas.appliancesTopic(INSTANCE_ID);

    private static Path certDir;
    private static Path matchingKeystore;
    private static Path matchingPem;
    private static Path mismatchKeystore;
    private static Path mismatchPem;

    private final List<String> julLines = new CopyOnWriteArrayList<>();
    private Handler julHandler;
    private ch.qos.logback.classic.Logger mqttLogger;
    private ListAppender<ILoggingEvent> appender;
    private Path passwordFile;
    private EmbeddedMqttBroker broker;
    private MqttClient subscriber;
    private final List<Captured> messages = new CopyOnWriteArrayList<>();
    private PahoMqttConnector connector;
    private Thread worker;

    @BeforeAll
    static void certificates() throws Exception {
        certDir = Files.createTempDirectory("mqtt-certs-");
        matchingKeystore = certDir.resolve("matching.p12");
        matchingPem = certDir.resolve("matching.pem");
        mismatchKeystore = certDir.resolve("mismatch.p12");
        mismatchPem = certDir.resolve("mismatch.pem");
        keytool("-genkeypair", "-alias", "broker", "-keyalg", "RSA", "-keysize", "2048", "-validity", "2",
                "-storetype", "PKCS12", "-keystore", matchingKeystore.toString(),
                "-storepass", STORE_PASS, "-keypass", STORE_PASS,
                "-dname", "CN=127.0.0.1", "-ext", "SAN=dns:localhost,ip:127.0.0.1", "-noprompt");
        keytool("-exportcert", "-rfc", "-alias", "broker", "-keystore", matchingKeystore.toString(),
                "-storepass", STORE_PASS, "-file", matchingPem.toString());
        keytool("-genkeypair", "-alias", "broker", "-keyalg", "RSA", "-keysize", "2048", "-validity", "2",
                "-storetype", "PKCS12", "-keystore", mismatchKeystore.toString(),
                "-storepass", STORE_PASS, "-keypass", STORE_PASS,
                "-dname", "CN=broker.invalid", "-ext", "SAN=dns:broker.invalid", "-noprompt");
        keytool("-exportcert", "-rfc", "-alias", "broker", "-keystore", mismatchKeystore.toString(),
                "-storepass", STORE_PASS, "-file", mismatchPem.toString());
    }

    @AfterAll
    static void deleteCertificates() throws IOException {
        if (certDir != null) {
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
    }

    @BeforeEach
    void logs() {
        mqttLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(MqttLog.CATEGORY);
        mqttLogger.setLevel(Level.DEBUG);
        appender = new ListAppender<>();
        appender.start();
        mqttLogger.addAppender(appender);
        julHandler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                julLines.add(record.getMessage() == null ? "" : record.getMessage());
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        julHandler.setLevel(java.util.logging.Level.ALL);
        Logger.getLogger("").addHandler(julHandler);
        passwordFile = null;
        broker = null;
        subscriber = null;
        connector = null;
        worker = null;
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
        if (subscriber != null) {
            try {
                subscriber.disconnectForcibly(0, 0, false);
                subscriber.close();
            } catch (Exception ignored) {
                // The broker may already be gone.
            }
        }
        if (broker != null) {
            broker.close();
        }
        if (julHandler != null) {
            Logger.getLogger("").removeHandler(julHandler);
        }
        if (mqttLogger != null && appender != null) {
            mqttLogger.detachAppender(appender);
        }
        assertSecretAbsent();
    }

    @Test
    void publishesTheContractBodyAndAcknowledgesIt() throws Exception {
        broker = startBroker(matchingKeystore, SECRET);
        subscriber = subscribe(matchingPem, SECRET);
        MutableSource source = new MutableSource(Map.of("fridge", true), 1);
        startPublisher(settings(broker.port(), matchingPem, Duration.ofHours(1)), source,
                ReconnectPolicy.standard(), Duration.ofMillis(50), Duration.ofMillis(200), 5);

        Captured captured = awaitMessage();
        assertEquals(TOPIC, captured.topic());
        assertEquals(ApplianceSchemas.QOS, captured.qos());
        assertFalse(captured.retained());
        String json = new String(captured.payload(), StandardCharsets.UTF_8);
        ApplianceStateMessage parsed = ApplianceSchemas.STRICT.readValue(json, ApplianceStateMessage.class);
        ApplianceJson.readState(json);
        assertEquals("Cabin", parsed.instanceName());
        assertEquals(null, parsed.appliedCommandId());
        assertEquals(1, parsed.appliances().size());
        assertEquals("fridge", parsed.appliances().get(0).applianceName());
        assertTrue(parsed.appliances().get(0).state());
        JsonNode node = ApplianceSchemas.STRICT.readTree(json);
        assertEquals(Set.of("instanceName", "appliedCommandId", "appliances"), fieldNames(node));
        assertTrue(node.get("appliedCommandId").isNull());
        assertFalse(node.has("instanceId"));
        assertFalse(node.has("on"));
        assertEquals(Set.of("applianceName", "state"), fieldNames(node.get("appliances").get(0)));
        assertTrue(node.get("appliances").get(0).get("state").isBoolean());

        awaitLog(line -> line.contains("MQTT settings")
                && line.contains("host=127.0.0.1")
                && line.contains("port=" + broker.port())
                && line.contains("username=autonomy")
                && line.contains("clientId=autonomy-" + INSTANCE_ID)
                && line.contains("instanceId=" + INSTANCE_ID)
                && line.contains("instanceName=Cabin")
                && line.contains("trust=ca-file")
                && line.contains("publishInterval="));
        awaitLog(line -> line.startsWith("connecting host="));
        awaitLog(line -> line.startsWith("connected host="));
        awaitLog(line -> line.startsWith("publish topic=" + TOPIC)
                && line.contains("appliances=1")
                && line.contains("bytes=")
                && !line.contains("{"));
        awaitLog(line -> line.contains("publish summary acknowledged=1"));
    }

    @Test
    void reconnectsAfterTheBrokerReturnsAndPublishesTheNewState() throws Exception {
        broker = startBroker(matchingKeystore, SECRET);
        subscriber = subscribe(matchingPem, SECRET);
        MutableSource source = new MutableSource(Map.of("fridge", false), 1);
        ReconnectPolicy policy = new ReconnectPolicy(Duration.ofSeconds(4), Duration.ofSeconds(4), 0, () -> 0);
        startPublisher(settings(broker.port(), matchingPem, Duration.ofHours(1)), source,
                policy, Duration.ofMillis(30), Duration.ofMinutes(1), 5);
        awaitMessage();

        broker.stop();
        closeSubscriber();
        awaitLog(line -> line.contains("connection lost"));
        awaitLog(line -> line.contains("reconnect attempt 1 in "));
        messages.clear();
        source.states.set(Map.of("fridge", true));
        broker.start();
        subscriber = subscribe(matchingPem, SECRET);

        Captured again = awaitMessage();
        String json = new String(again.payload(), StandardCharsets.UTF_8);
        ApplianceStateMessage parsed = ApplianceSchemas.STRICT.readValue(json, ApplianceStateMessage.class);
        assertEquals(TOPIC, again.topic());
        assertTrue(parsed.appliances().get(0).state());
        assertTrue(messages.stream().allMatch(message -> new String(message.payload(), StandardCharsets.UTF_8).contains("\"state\":true")));
    }

    @Test
    void badCredentialsAreNamedAndThePasswordIsNotLogged() throws Exception {
        broker = startBroker(matchingKeystore, "broker-expected");
        MutableSource source = new MutableSource(Map.of("fridge", false), 1);
        ReconnectPolicy policy = new ReconnectPolicy(Duration.ofMillis(40), Duration.ofMillis(40), 0, () -> 0);
        startPublisher(settings(broker.port(), matchingPem, Duration.ofHours(1)), source,
                policy, Duration.ofMillis(50), Duration.ofMinutes(1), 2);
        awaitLog(line -> line.contains("cause=bad credentials") && line.contains("reasonCode=4"));
        awaitLog(line -> line.contains("reconnect attempt 1 in "));
        awaitLog(line -> line.contains("MQTT consecutive failures attempt=2")
                && line.contains("nextDelay=")
                && line.contains("cause=bad credentials"));
    }

    @Test
    void untrustedCertificateIsNamed() throws Exception {
        broker = startBroker(matchingKeystore, SECRET);
        MutableSource source = new MutableSource(Map.of("fridge", false), 1);
        ReconnectPolicy policy = new ReconnectPolicy(Duration.ofMillis(50), Duration.ofMillis(50), 0, () -> 0);
        MqttSettings settings = settings(broker.port(), null, Duration.ofHours(1));
        startPublisher(settings, source, policy, Duration.ofMillis(50), Duration.ofMinutes(1), 5);
        awaitLog(line -> line.contains("trust=jvm-cacerts"));
        awaitLog(line -> line.contains("cause=certificate not trusted"));
    }

    @Test
    void refusesABrokerCertificateForADifferentHostname() throws Exception {
        broker = startBroker(mismatchKeystore, SECRET);
        MutableSource source = new MutableSource(Map.of("fridge", false), 1);
        ReconnectPolicy policy = new ReconnectPolicy(Duration.ofMillis(50), Duration.ofMillis(50), 0, () -> 0);
        startPublisher(settings(broker.port(), mismatchPem, Duration.ofHours(1)), source,
                policy, Duration.ofMillis(50), Duration.ofMinutes(1), 5);
        awaitLog(line -> line.contains("trust=ca-file"));
        awaitLog(line -> line.contains("cause=hostname mismatch"));
        awaitLog(line -> line.contains("reconnect attempt 1 in "));
        assertTrue(lines().stream().noneMatch(line -> line.contains("cause=certificate not trusted")));
        assertTrue(lines().stream().noneMatch(line -> line.startsWith("connected host=")));
    }

    @Test
    void passwordFileWithOneTrailingCrLfAndMode400Connects() throws Exception {
        broker = startBroker(matchingKeystore, SECRET);
        subscriber = subscribe(matchingPem, SECRET);
        MutableSource source = new MutableSource(Map.of("fridge", true), 1);
        startPublisher(settings(broker.port(), matchingPem, Duration.ofHours(1)), source,
                ReconnectPolicy.standard(), Duration.ofMillis(50), Duration.ofMinutes(1), 5,
                SECRET + "\r\n", "r--------");
        Captured captured = awaitMessage();
        assertEquals(TOPIC, captured.topic());
        awaitLog(line -> line.startsWith("connected host="));
    }

    @Test
    void passwordFileDoesNotTrimWhitespaceAroundTheNewline() throws Exception {
        broker = startBroker(matchingKeystore, SECRET);
        MutableSource source = new MutableSource(Map.of("fridge", false), 1);
        ReconnectPolicy policy = new ReconnectPolicy(Duration.ofMillis(40), Duration.ofMillis(40), 0, () -> 0);
        startPublisher(settings(broker.port(), matchingPem, Duration.ofHours(1)), source,
                policy, Duration.ofMillis(50), Duration.ofMinutes(1), 5,
                SECRET + " \n", "rw-------");
        awaitLog(line -> line.contains("cause=bad credentials") && line.contains("reasonCode=4"));
        assertTrue(lines().stream().noneMatch(line -> line.startsWith("connected host=")));
    }

    @Test
    @Timeout(5)
    void refusesAPasswordFileGroupOrOthersCanRead() throws Exception {
        MqttSettings configured = settings(1, matchingPem, Duration.ofHours(1));
        Files.writeString(passwordFile, SECRET + "\n");
        Files.setPosixFilePermissions(passwordFile, PosixFilePermissions.fromString("rw-r--r--"));
        connector = new PahoMqttConnector(
                ReconnectPolicy.standard(), Duration.ofMillis(20), Duration.ofMinutes(1), 5);
        connector.start(configured, new MutableSource(Map.of("fridge", false), 1));
        assertTrue(appender.list.stream().anyMatch(event -> event.getLevel() == Level.ERROR
                && event.getFormattedMessage().contains(
                        "MQTT off; password file is readable or writable by group or others path=")
                && event.getFormattedMessage().contains(passwordFile.toString())
                && !event.getFormattedMessage().contains(SECRET)));
        assertTrue(lines().stream().noneMatch(line -> line.startsWith("connecting host=")));
        assertTrue(lines().stream().noneMatch(line -> line.startsWith("MQTT settings")));
    }

    @Test
    void unreachableBrokerWarnsEveryNFailures() throws Exception {
        int closedPort = freePort();
        MutableSource source = new MutableSource(Map.of("fridge", false), 1);
        ReconnectPolicy policy = new ReconnectPolicy(Duration.ofMillis(20), Duration.ofMillis(20), 0, () -> 0);
        startPublisher(settings(closedPort, matchingPem, Duration.ofSeconds(1)), source,
                policy, Duration.ofMillis(20), Duration.ofMinutes(1), 2);
        awaitLog(line -> line.contains("cause=unreachable or timeout"));
        awaitLog(line -> line.contains("MQTT consecutive failures attempt=2")
                && line.contains("cause=unreachable or timeout"));
    }

    @Test
    void emptyCacheSkipsPublishUntilAPinIsCached() throws Exception {
        broker = startBroker(matchingKeystore, SECRET);
        subscriber = subscribe(matchingPem, SECRET);
        MutableSource source = new MutableSource(Map.of(), 1);
        startPublisher(settings(broker.port(), matchingPem, Duration.ofHours(1)), source,
                ReconnectPolicy.standard(), Duration.ofMillis(40), Duration.ofMinutes(1), 5);
        awaitLog(line -> line.contains("appliance cache empty; skipping publish"));
        Thread.sleep(150);
        assertTrue(messages.isEmpty());
        source.states.set(Map.of("fridge", false));
        Captured captured = awaitMessage();
        ApplianceStateMessage parsed = ApplianceSchemas.STRICT.readValue(
                new String(captured.payload(), StandardCharsets.UTF_8), ApplianceStateMessage.class);
        assertFalse(parsed.appliances().get(0).state());
    }

    private EmbeddedMqttBroker startBroker(Path keystore, String password) throws Exception {
        EmbeddedMqttBroker started = new EmbeddedMqttBroker(freePort(), keystore, STORE_PASS, USER, password);
        started.start();
        return started;
    }

    private void startPublisher(MqttSettings settings, ApplianceStateSource source, ReconnectPolicy policy,
            Duration poll, Duration summary, int warnEvery) throws IOException {
        startPublisher(settings, source, policy, poll, summary, warnEvery, SECRET + "\n", "rw-------");
    }

    private void startPublisher(MqttSettings settings, ApplianceStateSource source, ReconnectPolicy policy,
            Duration poll, Duration summary, int warnEvery, String passwordContents, String mode) throws IOException {
        Files.writeString(passwordFile, passwordContents);
        Files.setPosixFilePermissions(passwordFile, PosixFilePermissions.fromString(mode));
        connector = new PahoMqttConnector(policy, poll, summary, warnEvery);
        worker = new Thread(() -> connector.start(settings, source), "test-mqtt");
        worker.setDaemon(true);
        worker.start();
    }

    private MqttSettings settings(int port, Path caFile, Duration publishInterval) throws IOException {
        passwordFile = Files.createTempFile("mqtt-password-", ".pass");
        Path file = passwordFile;
        String clientId = "autonomy-" + INSTANCE_ID;
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
                return port;
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
                return Optional.ofNullable(caFile);
            }

            @Override
            public String clientId() {
                return clientId;
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
                return publishInterval;
            }

            @Override
            public Duration keepalive() {
                return Duration.ofSeconds(30);
            }

            @Override
            public Duration connectTimeout() {
                return Duration.ofSeconds(2);
            }
        };
    }

    private MqttClient subscribe(Path caFile, String password) throws Exception {
        MqttClient client = new MqttClient("ssl://127.0.0.1:" + broker.port(), "sub-" + UUID.randomUUID(),
                new MemoryPersistence());
        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                messages.add(new Captured(topic, message.getPayload(), message.getQos(), message.isRetained()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
            }
        });
        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(false);
        options.setCleanSession(true);
        options.setUserName(USER);
        options.setPassword(password.toCharArray());
        options.setConnectionTimeout(5);
        options.setHttpsHostnameVerificationEnabled(true);
        options.setSocketFactory(MqttTrust.socketFactory(Optional.of(caFile)));
        client.connect(options);
        client.subscribe(TOPIC, ApplianceSchemas.QOS);
        return client;
    }

    private void closeSubscriber() {
        if (subscriber == null) {
            return;
        }
        try {
            subscriber.disconnectForcibly(0, 0, false);
            subscriber.close();
        } catch (Exception ignored) {
            // Broker is already stopped.
        }
        subscriber = null;
    }

    private Captured awaitMessage() {
        await(Duration.ofSeconds(8), () -> !messages.isEmpty());
        return messages.get(messages.size() - 1);
    }

    private void awaitLog(java.util.function.Predicate<String> match) {
        await(Duration.ofSeconds(12), () -> lines().stream().anyMatch(match));
    }

    private void await(Duration timeout, BooleanSupplier condition) {
        long end = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < timeoutNanos(end)) {
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

    private static long timeoutNanos(long end) {
        return end;
    }

    private List<String> lines() {
        return new ArrayList<>(appender.list).stream().map(ILoggingEvent::getFormattedMessage).toList();
    }

    private void assertSecretAbsent() {
        for (String line : lines()) {
            assertFalse(line.contains(SECRET), line);
        }
        for (String line : julLines) {
            assertFalse(line.contains(SECRET), line);
        }
    }

    private static Set<String> fieldNames(JsonNode node) {
        Set<String> names = new java.util.LinkedHashSet<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
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

    private static final class MutableSource implements ApplianceStateSource {
        private final AtomicReference<Map<String, Boolean>> states;
        private final int mappedCount;

        private MutableSource(Map<String, Boolean> states, int mappedCount) {
            this.states = new AtomicReference<>(states);
            this.mappedCount = mappedCount;
        }

        @Override
        public Snapshot read() {
            return new Snapshot(states.get(), mappedCount);
        }
    }

    private record Captured(String topic, byte[] payload, int qos, boolean retained) {
    }
}
