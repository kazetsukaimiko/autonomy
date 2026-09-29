package io.freedriver.autonomy.mqtt.impl;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.freedriver.autonomy.mqtt.ApplianceStateSource;
import io.freedriver.autonomy.mqtt.MqttConnector;
import io.freedriver.autonomy.mqtt.MqttLog;
import io.freedriver.autonomy.mqtt.MqttSettings;
import io.freedriver.mqtt.contract.Appliance;
import io.freedriver.mqtt.contract.ApplianceJson;
import io.freedriver.mqtt.contract.ApplianceSchemas;
import io.freedriver.mqtt.contract.ApplianceStateMessage;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Paho publisher. Each connection is a new clean session, so a previous payload
 * is not replayed; the current snapshot is published after every connect.
 */
public final class PahoMqttConnector implements MqttConnector {

    static final Duration POLL_INTERVAL = Duration.ofSeconds(1);
    static final Duration SUMMARY_INTERVAL = Duration.ofMinutes(1);
    /** WARN cadence for consecutive connect failures. */
    static final int WARN_EVERY = 5;

    private static final Logger LOG = LoggerFactory.getLogger(MqttLog.CATEGORY);

    private final ReconnectPolicy reconnect;
    private final Duration pollInterval;
    private final Duration summaryInterval;
    private final int warnEvery;

    private final AtomicBoolean stopped = new AtomicBoolean();
    private final AtomicReference<Throwable> connectionLoss = new AtomicReference<>();
    private volatile Thread worker;
    private volatile MqttClient client;

    private int acknowledged;
    private int failed;
    private Instant nextSummary = Instant.EPOCH;
    private Instant lastStateWarn = Instant.EPOCH;
    private Map<String, Boolean> lastSnapshot;
    private Instant lastPublishedAt = Instant.EPOCH;
    private boolean loggedEmptyCache;
    private final Set<String> skippedNames = new HashSet<>();

    public PahoMqttConnector() {
        this(ReconnectPolicy.standard(), POLL_INTERVAL, SUMMARY_INTERVAL, WARN_EVERY);
    }

    public PahoMqttConnector(ReconnectPolicy reconnect, Duration pollInterval, Duration summaryInterval, int warnEvery) {
        this.reconnect = reconnect;
        this.pollInterval = pollInterval;
        this.summaryInterval = summaryInterval;
        this.warnEvery = warnEvery;
    }

    @Override
    public void start(MqttSettings settings, ApplianceStateSource states) {
        Path passwordFile = settings.passwordFile();
        if (Files.isRegularFile(passwordFile) && !PasswordFiles.ownerOnly(passwordFile)) {
            refusePasswordFile(passwordFile);
            return;
        }
        worker = Thread.currentThread();
        logSettings(settings);
        nextSummary = Instant.now().plus(summaryInterval);
        int attempt = 0;
        while (!stopped.get()) {
            try {
                openSession(settings);
                attempt = 0;
                runSession(settings, states);
            } catch (PasswordFilePermissions rejected) {
                refusePasswordFile(rejected.path());
                break;
            } catch (ConnectionDropped dropped) {
                if (stopped.get()) {
                    break;
                }
                String cause = MqttFailures.describe(dropped.reason).cause();
                retry(++attempt, cause);
            } catch (Exception e) {
                if (stopped.get() || Thread.currentThread().isInterrupted()) {
                    break;
                }
                MqttFailures.Description failure = MqttFailures.describe(e);
                LOG.info("connect failed reasonCode={} causeClass={} cause={}",
                        failure.reasonCode(), failure.causeClass(), failure.cause());
                retry(++attempt, failure.cause());
            }
        }
        closeCurrentClient();
    }

    @Override
    public void close() {
        stopped.set(true);
        closeCurrentClient();
        Thread current = worker;
        if (current != null) {
            current.interrupt();
        }
    }

    private static void refusePasswordFile(Path path) {
        LOG.error("MQTT off; password file is readable or writable by group or others path={}", path);
    }

    private void logSettings(MqttSettings settings) {
        String trust = settings.caFile().isPresent() ? "ca-file" : "jvm-cacerts";
        LOG.info("MQTT settings host={} port={} clientId={} instanceId={} instanceName={} trust={} publishInterval={}",
                settings.host(),
                settings.port(),
                settings.clientId(),
                settings.instanceId(),
                settings.instanceName(),
                trust,
                settings.publishInterval());
    }

    private void openSession(MqttSettings settings) throws Exception {
        char[] password = PasswordFiles.read(settings.passwordFile());
        try {
            openSession(settings, password);
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private void openSession(MqttSettings settings, char[] password) throws Exception {
        LOG.info("connecting host={} port={}", settings.host(), settings.port());
        connectionLoss.set(null);
        lastSnapshot = null;
        lastPublishedAt = Instant.EPOCH;
        loggedEmptyCache = false;
        MqttClient created = new MqttClient(serverUri(settings), settings.clientId(), new MemoryPersistence());
        created.setTimeToWait(Math.max(1000L, settings.connectTimeout().toMillis()));
        created.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                Throwable reason = cause == null ? new java.io.IOException("connection lost") : cause;
                if (connectionLoss.compareAndSet(null, reason)) {
                    MqttFailures.Description failure = MqttFailures.describe(reason);
                    LOG.info("connection lost reasonCode={} causeClass={} cause={}",
                            failure.reasonCode(), failure.causeClass(), failure.cause());
                }
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                // Publish-only. Command subscription is a later card.
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                // QoS 1 publish returns when PUBACK arrives; the count is taken there.
            }
        });
        this.client = created;
        if (stopped.get()) {
            closeCurrentClient();
            return;
        }
        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(false);
        options.setCleanSession(true);
        options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
        options.setUserName(settings.username());
        options.setKeepAliveInterval(wholeSeconds(settings.keepalive()));
        options.setConnectionTimeout(wholeSeconds(settings.connectTimeout()));
        options.setHttpsHostnameVerificationEnabled(true);
        options.setSocketFactory(MqttTrust.socketFactory(settings.caFile()));
        options.setPassword(password);
        created.connect(options);
        if (stopped.get()) {
            closeCurrentClient();
            return;
        }
        LOG.info("connected host={} port={}", settings.host(), settings.port());
    }

    private void runSession(MqttSettings settings, ApplianceStateSource states) throws Exception {
        MqttClient current = client;
        if (current == null || !current.isConnected()) {
            return;
        }
        while (!stopped.get()) {
            Throwable lost = connectionLoss.get();
            if (lost != null || client == null || !client.isConnected()) {
                throw new ConnectionDropped(lost);
            }
            maybeSummarize();
            publishCurrent(settings, states);
            sleep(pollInterval);
        }
    }

    private void publishCurrent(MqttSettings settings, ApplianceStateSource states) throws ConnectionDropped {
        ApplianceStateSource.Snapshot snapshot;
        try {
            snapshot = states.read();
        } catch (Exception e) {
            noteStateSource(e);
            return;
        }
        Map<String, Boolean> publishable = publishable(snapshot.states());
        if (publishable.isEmpty() && snapshot.mappedCount() > 0) {
            if (!loggedEmptyCache) {
                LOG.info("appliance cache empty; skipping publish");
                loggedEmptyCache = true;
            }
            return;
        }
        loggedEmptyCache = false;
        boolean due = !Instant.now().isBefore(lastPublishedAt.plus(settings.publishInterval()));
        if (publishable.equals(lastSnapshot) && !due) {
            return;
        }
        String topic = ApplianceSchemas.appliancesTopic(settings.instanceId());
        try {
            ApplianceStateMessage message = new ApplianceStateMessage(
                    settings.instanceName(), null, appliances(publishable));
            byte[] body = ApplianceJson.writeState(message).getBytes(StandardCharsets.UTF_8);
            MqttClient current = client;
            if (current == null || !current.isConnected()) {
                throw new ConnectionDropped(connectionLoss.get());
            }
            // QoS 1 blocks until PUBACK. That return is what "sent" means.
            current.publish(topic, body, ApplianceSchemas.QOS, ApplianceSchemas.RETAIN);
            acknowledged++;
            lastSnapshot = publishable;
            lastPublishedAt = Instant.now();
            LOG.debug("publish topic={} appliances={} bytes={}", topic, publishable.size(), body.length);
        } catch (ConnectionDropped dropped) {
            failed++;
            throw dropped;
        } catch (Exception e) {
            failed++;
            LOG.debug("publish failed topic={} causeClass={}", topic, e.getClass().getName());
            if (connectionLoss.get() != null || client == null || !client.isConnected()) {
                throw new ConnectionDropped(e);
            }
        }
    }

    private Map<String, Boolean> publishable(Map<String, Boolean> states) {
        java.util.LinkedHashMap<String, Boolean> kept = new java.util.LinkedHashMap<>();
        states.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.nullsLast(Comparator.naturalOrder())))
                .forEach(entry -> {
                    String name = entry.getKey();
                    Boolean state = entry.getValue();
                    if (name == null || name.isBlank() || name.length() > ApplianceSchemas.NAME_MAX || state == null) {
                        if (name != null && skippedNames.add(name)) {
                            LOG.warn("skipping appliance name that fails the MQTT contract");
                        }
                        return;
                    }
                    kept.put(name, state);
                });
        return Map.copyOf(kept);
    }

    private static List<Appliance> appliances(Map<String, Boolean> states) {
        List<Appliance> appliances = new ArrayList<>(states.size());
        states.forEach((name, state) -> appliances.add(new Appliance(name, state)));
        return List.copyOf(appliances);
    }

    private void noteStateSource(Exception error) {
        Instant now = Instant.now();
        if (lastStateWarn.equals(Instant.EPOCH) || !now.isBefore(lastStateWarn.plus(summaryInterval))) {
            LOG.warn("state source failed causeClass={}", error.getClass().getName());
            lastStateWarn = now;
        }
    }

    private void maybeSummarize() {
        if (Instant.now().isBefore(nextSummary)) {
            return;
        }
        LOG.info("publish summary acknowledged={} failed={}", acknowledged, failed);
        acknowledged = 0;
        failed = 0;
        nextSummary = Instant.now().plus(summaryInterval);
    }

    private void retry(int attempt, String cause) {
        closeCurrentClient();
        if (stopped.get()) {
            return;
        }
        Duration delay = reconnect.delay(attempt);
        LOG.info("reconnect attempt {} in {}", attempt, delay.toMillis() + "ms");
        if (attempt % warnEvery == 0) {
            LOG.warn("MQTT consecutive failures attempt={} nextDelay={} cause={}",
                    attempt, delay.toMillis() + "ms", cause);
        }
        sleep(delay);
        maybeSummarize();
    }

    private void sleep(Duration delay) {
        if (stopped.get()) {
            return;
        }
        try {
            Thread.sleep(delay.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            stopped.set(true);
        }
    }

    private void closeCurrentClient() {
        MqttClient current = client;
        client = null;
        if (current == null) {
            return;
        }
        try {
            current.disconnectForcibly(0, 0, false);
        } catch (Exception ignored) {
            // Already gone.
        }
        try {
            current.close();
        } catch (Exception ignored) {
            // Already gone.
        }
    }

    private static String serverUri(MqttSettings settings) {
        return "ssl://" + settings.host() + ":" + settings.port();
    }

    private static int wholeSeconds(Duration duration) {
        long seconds = duration.toSeconds();
        if (seconds < 1) {
            return 1;
        }
        if (seconds > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) seconds;
    }

    private static final class ConnectionDropped extends Exception {
        private final Throwable reason;

        private ConnectionDropped(Throwable reason) {
            this.reason = reason;
        }
    }
}
