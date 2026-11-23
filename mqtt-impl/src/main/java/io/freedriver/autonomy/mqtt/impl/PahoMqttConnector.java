package io.freedriver.autonomy.mqtt.impl;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.freedriver.autonomy.mqtt.ApplianceCommandSink;
import io.freedriver.autonomy.mqtt.ApplianceStateSource;
import io.freedriver.autonomy.mqtt.CommandListResult;
import io.freedriver.autonomy.mqtt.CommandListSink;
import io.freedriver.autonomy.mqtt.MqttConnector;
import io.freedriver.autonomy.mqtt.MqttLog;
import io.freedriver.autonomy.mqtt.MqttSettings;
import io.freedriver.autonomy.mqtt.PendingSource;
import io.freedriver.autonomy.mqtt.ScheduledListener;
import io.freedriver.mqtt.contract.Appliance;
import io.freedriver.mqtt.contract.ApplianceCommandList;
import io.freedriver.mqtt.contract.ApplianceCommandMessage;
import io.freedriver.mqtt.contract.ApplianceJson;
import io.freedriver.mqtt.contract.ApplianceSchemas;
import io.freedriver.mqtt.contract.ApplianceStateMessage;
import io.freedriver.mqtt.contract.PendingCommand;
import io.freedriver.mqtt.contract.ScheduledCommandsMessage;
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
 * Command subscribe and command-list subscribe follow commands-enabled and use
 * the exact topics. A refused command-list subscribe or scheduled publish is
 * retried on its own and leaves the commands subscription in place.
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
    private final Object publishLock = new Object();
    private final Object commandLock = new Object();
    private final LinkedHashMap<String, CommandWork> pending = new LinkedHashMap<>();
    private final ArrayDeque<ApplyWork> inbox = new ArrayDeque<>();
    private final ArrayDeque<String> appliedCommandIds = new ArrayDeque<>();
    private volatile Thread worker;
    private volatile Thread commandThread;
    private volatile MqttClient client;
    private volatile ApplianceCommandSink commandSink;
    private volatile CommandListSink commandLists;
    private volatile PendingSource pendingSource;
    private volatile boolean scheduleSnapshots;
    private volatile boolean scheduledDue;
    private volatile boolean commandsPaused;
    private boolean commandListsSubscribed;
    private int listSubscribeFailures;
    private Instant listSubscribeNotBefore = Instant.EPOCH;
    private int scheduledFailures;
    private Instant scheduledNotBefore = Instant.EPOCH;

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
        start(settings, states, null, null, null);
    }

    @Override
    public void start(MqttSettings settings, ApplianceStateSource states, ApplianceCommandSink commands) {
        start(settings, states, commands, null, null);
    }

    @Override
    public void start(MqttSettings settings, ApplianceStateSource states, ApplianceCommandSink commands,
            CommandListSink lists, PendingSource pending) {
        this.commandSink = commands;
        this.commandLists = lists;
        this.pendingSource = pending;
        this.scheduleSnapshots = settings.commandsEnabled() && pending != null;
        Path passwordFile = settings.passwordFile();
        if (Files.isRegularFile(passwordFile) && !PasswordFiles.ownerOnly(passwordFile)) {
            refusePasswordFile(passwordFile);
            return;
        }
        worker = Thread.currentThread();
        logSettings(settings);
        nextSummary = Instant.now().plus(summaryInterval);
        if (settings.commandsEnabled()) {
            if (commands == null && lists == null) {
                LOG.info("commands not subscribed; no command handler");
            } else {
                Thread applying = new Thread(() -> runCommands(settings, states, commands), "mqtt-commands");
                applying.setDaemon(true);
                commandThread = applying;
                applying.start();
            }
        }
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
        stopCommandThread();
        closeCurrentClient();
    }

    @Override
    public ScheduledListener scheduledListener() {
        return this::entryFired;
    }

    private void entryFired(UUID entryId) {
        if (entryId == null) {
            throw new IllegalArgumentException("entryId");
        }
        synchronized (publishLock) {
            appliedCommandIds.addLast(entryId.toString());
        }
        markScheduledDue();
    }

    @Override
    public void publishExact(String topic, byte[] payload, int qos, boolean retain) {
        if (topic == null || topic.isBlank() || topic.indexOf('+') >= 0 || topic.indexOf('#') >= 0) {
            throw new IllegalArgumentException("exact topic only");
        }
        synchronized (publishLock) {
            MqttClient current = client;
            if (stopped.get() || current == null || !current.isConnected()) {
                throw new IllegalStateException("MQTT not connected");
            }
            try {
                current.publish(topic, payload == null ? new byte[0] : payload, qos, retain);
            } catch (Exception e) {
                throw new IllegalStateException("MQTT publish failed", e);
            }
        }
    }

    @Override
    public void close() {
        stopped.set(true);
        synchronized (commandLock) {
            commandLock.notifyAll();
        }
        closeCurrentClient();
        Thread current = worker;
        if (current != null) {
            current.interrupt();
        }
        Thread applying = commandThread;
        if (applying != null) {
            applying.interrupt();
        }
    }

    /** Holds command applies so a test can queue more than one before the first runs. */
    void pauseCommandApplies() {
        commandsPaused = true;
    }

    void resumeCommandApplies() {
        synchronized (commandLock) {
            commandsPaused = false;
            commandLock.notifyAll();
        }
    }

    static String commandTopic(UUID instanceId) {
        return exactTopic(ApplianceSchemas.commandsTopic(instanceId), "command topic must not be a wildcard");
    }

    static String commandListsTopic(UUID instanceId) {
        return exactTopic(ApplianceSchemas.commandListsTopic(instanceId), "command-lists topic must not be a wildcard");
    }

    static String scheduledTopic(UUID instanceId) {
        return exactTopic(ApplianceSchemas.scheduledTopic(instanceId), "scheduled topic must not be a wildcard");
    }

    private static String exactTopic(String topic, String message) {
        if (topic.indexOf('+') >= 0 || topic.indexOf('#') >= 0) {
            throw new IllegalStateException(message);
        }
        return topic;
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
        synchronized (publishLock) {
            lastSnapshot = null;
            lastPublishedAt = Instant.EPOCH;
            loggedEmptyCache = false;
        }
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
                onCommand(settings, topic, message);
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
        commandListsSubscribed = false;
        listSubscribeFailures = 0;
        listSubscribeNotBefore = Instant.EPOCH;
        scheduledFailures = 0;
        scheduledNotBefore = Instant.EPOCH;
        scheduledDue = scheduleSnapshots;
        subscribeCommands(created, settings);
        subscribeCommandLists(created, settings);
        LOG.info("connected host={} port={}", settings.host(), settings.port());
    }

    private void subscribeCommands(MqttClient created, MqttSettings settings) throws Exception {
        if (!settings.commandsEnabled() || commandSink == null || commandThread == null) {
            return;
        }
        String topic = commandTopic(settings.instanceId());
        created.subscribe(topic, ApplianceSchemas.QOS);
        LOG.info("commands subscribed topic={} qos={} retain=false", topic, ApplianceSchemas.QOS);
    }

    /**
     * A refused SUBACK is logged and retried later. It does not unsubscribe
     * commands and does not fail the session while the client stays connected.
     */
    private void subscribeCommandLists(MqttClient created, MqttSettings settings) throws ConnectionDropped {
        if (created == null || !created.isConnected() || !settings.commandsEnabled() || commandLists == null
                || commandThread == null || commandListsSubscribed) {
            return;
        }
        if (Instant.now().isBefore(listSubscribeNotBefore)) {
            return;
        }
        String topic = commandListsTopic(settings.instanceId());
        try {
            created.subscribe(topic, ApplianceSchemas.QOS);
            commandListsSubscribed = true;
            listSubscribeFailures = 0;
            listSubscribeNotBefore = Instant.EPOCH;
            LOG.info("command-lists subscribed topic={} qos={} retain=false", topic, ApplianceSchemas.QOS);
        } catch (Exception e) {
            listSubscribeFailures++;
            Duration delay = reconnect.delay(listSubscribeFailures);
            listSubscribeNotBefore = Instant.now().plus(delay);
            MqttFailures.Description failure = MqttFailures.describe(e);
            LOG.info("command-lists subscribe refused topic={} reasonCode={} causeClass={} cause={}",
                    topic, failure.reasonCode(), failure.causeClass(), failure.cause());
            if (!created.isConnected()) {
                throw new ConnectionDropped(e);
            }
        }
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
            subscribeCommandLists(client, settings);
            publishCurrent(settings, states);
            sleep(pollInterval);
        }
    }

    private void publishCurrent(MqttSettings settings, ApplianceStateSource states) throws ConnectionDropped {
        synchronized (publishLock) {
            publishUnderLock(settings, states);
            publishScheduledUnderLock(settings);
        }
    }

    private void publishApplied(MqttSettings settings, ApplianceStateSource states, String commandId) {
        synchronized (publishLock) {
            if (commandId != null) {
                appliedCommandIds.addLast(commandId);
            }
            try {
                publishUnderLock(settings, states);
                publishScheduledUnderLock(settings);
            } catch (ConnectionDropped dropped) {
                // The id stays queued when the send did not complete.
            }
        }
    }

    private void markScheduledDue() {
        if (scheduleSnapshots) {
            scheduledDue = true;
        }
    }

    /**
     * QoS 1, retain false. An empty pending list is a real publish. A refusal
     * while the client is still connected is logged and retried on its own.
     */
    private void publishScheduledUnderLock(MqttSettings settings) throws ConnectionDropped {
        if (!scheduleSnapshots || !scheduledDue || Instant.now().isBefore(scheduledNotBefore)) {
            return;
        }
        String topic = scheduledTopic(settings.instanceId());
        MqttClient current = client;
        if (current == null || !current.isConnected()) {
            throw new ConnectionDropped(connectionLoss.get());
        }
        List<PendingCommand> rows;
        try {
            rows = pendingSource.pending();
            if (rows == null) {
                rows = List.of();
            }
        } catch (RuntimeException e) {
            scheduledFailures++;
            scheduledNotBefore = Instant.now().plus(reconnect.delay(scheduledFailures));
            LOG.info("scheduled publish skipped topic={} reason=pending unavailable", topic);
            return;
        }
        try {
            ScheduledCommandsMessage message = new ScheduledCommandsMessage(Instant.now(), rows);
            byte[] body = ApplianceJson.writeScheduled(message).getBytes(StandardCharsets.UTF_8);
            current.publish(topic, body, ApplianceSchemas.QOS, false);
            scheduledDue = false;
            scheduledFailures = 0;
            scheduledNotBefore = Instant.EPOCH;
            LOG.info("scheduled published topic={} pending={} qos=1 retain=false", topic, rows.size());
        } catch (Exception e) {
            if (connectionLoss.get() != null || !current.isConnected()) {
                throw new ConnectionDropped(e);
            }
            scheduledFailures++;
            scheduledNotBefore = Instant.now().plus(reconnect.delay(scheduledFailures));
            MqttFailures.Description failure = MqttFailures.describe(e);
            LOG.info("scheduled publish refused topic={} causeClass={} cause={}",
                    topic, failure.causeClass(), failure.cause());
        }
    }

    private void publishUnderLock(MqttSettings settings, ApplianceStateSource states) throws ConnectionDropped {
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
        boolean commandPending = !appliedCommandIds.isEmpty();
        boolean due = commandPending || !Instant.now().isBefore(lastPublishedAt.plus(settings.publishInterval()));
        if (publishable.equals(lastSnapshot) && !due) {
            return;
        }
        String topic = ApplianceSchemas.appliancesTopic(settings.instanceId());
        String commandId = appliedCommandIds.pollFirst();
        try {
            ApplianceStateMessage message = new ApplianceStateMessage(
                    settings.instanceName(), commandId, appliances(publishable));
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
            if (commandId != null) {
                appliedCommandIds.addFirst(commandId);
            }
            failed++;
            throw dropped;
        } catch (Exception e) {
            if (commandId != null) {
                appliedCommandIds.addFirst(commandId);
            }
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
        int acknowledgedCount;
        int failedCount;
        synchronized (publishLock) {
            acknowledgedCount = acknowledged;
            failedCount = failed;
            acknowledged = 0;
            failed = 0;
        }
        LOG.info("publish summary acknowledged={} failed={}", acknowledgedCount, failedCount);
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

    private void stopCommandThread() {
        stopped.set(true);
        synchronized (commandLock) {
            commandLock.notifyAll();
        }
        Thread applying = commandThread;
        if (applying == null) {
            return;
        }
        applying.interrupt();
        boolean interrupted = Thread.interrupted();
        try {
            applying.join(2_000);
        } catch (InterruptedException e) {
            interrupted = true;
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    void onCommand(MqttSettings settings, String topic, MqttMessage message) {
        try {
            if (isCommandListTopic(settings, topic)) {
                acceptList(settings, topic, message);
                return;
            }
            acceptCommand(settings, topic, message);
        } catch (RuntimeException e) {
            LOG.info("command rejected topic={} reason=malformed", topic);
        }
    }

    private static boolean isCommandListTopic(MqttSettings settings, String topic) {
        return topic != null && topic.equals(commandListsTopic(settings.instanceId()));
    }

    private void acceptList(MqttSettings settings, String topic, MqttMessage message) {
        if (!settings.commandsEnabled() || commandLists == null || commandThread == null) {
            return;
        }
        if (message.isRetained()) {
            LOG.info("command list rejected topic={} reason=retained", topic);
            return;
        }
        byte[] payload = message.getPayload();
        int size = payload == null ? 0 : payload.length;
        if (size > settings.commandMaxBytes()) {
            LOG.info("command list rejected topic={} reason=payload too large", topic);
            return;
        }
        ApplianceCommandList parsed;
        try {
            byte[] body = payload == null ? new byte[0] : payload;
            parsed = ApplianceJson.readCommandList(new String(body, StandardCharsets.UTF_8));
        } catch (RuntimeException e) {
            LOG.info("command list rejected topic={} reason=malformed", topic);
            return;
        }
        LOG.info("command list received topic={} reason=received", topic);
        offerList(topic, parsed, Instant.now());
    }

    private void acceptCommand(MqttSettings settings, String topic, MqttMessage message) {
        if (!settings.commandsEnabled() || commandSink == null || commandThread == null) {
            return;
        }
        String expected = commandTopic(settings.instanceId());
        if (topic == null || !topic.equals(expected)) {
            LOG.info("command rejected topic={} reason=unexpected topic", topic);
            return;
        }
        if (message.isRetained()) {
            LOG.info("command rejected topic={} reason=retained", topic);
            return;
        }
        byte[] payload = message.getPayload();
        int size = payload == null ? 0 : payload.length;
        if (size > settings.commandMaxBytes()) {
            LOG.info("command rejected topic={} reason=payload too large", topic);
            return;
        }
        ApplianceCommandMessage parsed;
        try {
            byte[] body = payload == null ? new byte[0] : payload;
            parsed = ApplianceJson.readCommand(new String(body, StandardCharsets.UTF_8));
        } catch (RuntimeException e) {
            LOG.info("command rejected topic={} reason=malformed", topic);
            return;
        }
        LOG.info("command received topic={} reason=received", topic);
        offer(topic, parsed);
    }

    private void offer(String topic, ApplianceCommandMessage parsed) {
        synchronized (commandLock) {
            CommandWork previous = pending.remove(parsed.applianceName());
            if (previous != null) {
                inbox.remove(previous);
                LOG.info("command rejected topic={} reason=superseded", previous.queued().topic());
            }
            CommandWork next = new CommandWork(new QueuedCommand(topic, parsed));
            pending.put(parsed.applianceName(), next);
            inbox.addLast(next);
            commandLock.notifyAll();
        }
    }

    private void offerList(String topic, ApplianceCommandList parsed, Instant receivedAt) {
        synchronized (commandLock) {
            inbox.addLast(new ListWork(topic, parsed, receivedAt));
            commandLock.notifyAll();
        }
    }

    private void runCommands(MqttSettings settings, ApplianceStateSource states, ApplianceCommandSink sink) {
        while (!stopped.get()) {
            List<ApplyWork> batch = takeBatch();
            if (batch == null) {
                return;
            }
            for (ApplyWork work : batch) {
                if (stopped.get()) {
                    return;
                }
                if (work instanceof CommandWork command) {
                    applyOne(settings, states, sink, command.queued());
                } else if (work instanceof ListWork list) {
                    applyList(settings, states, list);
                }
            }
        }
    }

    private List<ApplyWork> takeBatch() {
        synchronized (commandLock) {
            while (!stopped.get() && (commandsPaused || inbox.isEmpty())) {
                try {
                    commandLock.wait(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    stopped.set(true);
                    return null;
                }
            }
            if (stopped.get()) {
                return null;
            }
            List<ApplyWork> batch = List.copyOf(inbox);
            inbox.clear();
            pending.clear();
            return batch;
        }
    }

    private void applyOne(MqttSettings settings, ApplianceStateSource states, ApplianceCommandSink sink,
            QueuedCommand command) {
        boolean applied;
        try {
            applied = sink.apply(command.message().applianceName(), command.message().state());
        } catch (Exception e) {
            LOG.info("command rejected topic={} reason=apply failed", command.topic());
            return;
        }
        if (!applied) {
            LOG.info("command rejected topic={} reason=unknown appliance", command.topic());
            return;
        }
        LOG.info("command applied topic={} reason=applied", command.topic());
        publishApplied(settings, states, command.message().commandId());
    }

    private void applyList(MqttSettings settings, ApplianceStateSource states, ListWork work) {
        CommandListSink sink = commandLists;
        if (sink == null) {
            LOG.info("command list rejected topic={} reason=apply failed", work.topic());
            return;
        }
        CommandListResult result;
        try {
            result = sink.accept(work.list(), work.receivedAt());
        } catch (RuntimeException e) {
            LOG.info("command list rejected topic={} reason=apply failed", work.topic());
            return;
        }
        switch (result) {
            case CommandListResult.Applied applied -> {
                LOG.info("command list applied topic={} reason=applied", work.topic());
                markScheduledDue();
                publishApplied(settings, states, applied.listId());
            }
            case CommandListResult.Refused refused ->
                    LOG.info("command list rejected topic={} reason={}", work.topic(), shortReason(refused.reason()));
            case CommandListResult.Dropped dropped ->
                    LOG.info("command list rejected topic={} reason={}", work.topic(), shortReason(dropped.reason()));
            case null -> LOG.info("command list rejected topic={} reason=apply failed", work.topic());
        }
    }

    private static String shortReason(String reason) {
        String trimmed = reason == null ? "rejected" : reason.trim().replace('\n', ' ');
        if (trimmed.length() > 80) {
            return trimmed.substring(0, 80);
        }
        return trimmed;
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

    private sealed interface ApplyWork permits CommandWork, ListWork {
    }

    private record CommandWork(QueuedCommand queued) implements ApplyWork {
    }

    private record ListWork(String topic, ApplianceCommandList list, Instant receivedAt) implements ApplyWork {
    }

    private record QueuedCommand(String topic, ApplianceCommandMessage message) {
    }

    private static final class ConnectionDropped extends Exception {
        private final Throwable reason;

        private ConnectionDropped(Throwable reason) {
            this.reason = reason;
        }
    }
}
