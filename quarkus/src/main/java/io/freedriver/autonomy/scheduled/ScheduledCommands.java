package io.freedriver.autonomy.scheduled;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.freedriver.autonomy.mqtt.CommandListResult;
import io.freedriver.autonomy.mqtt.CommandListSink;
import io.freedriver.autonomy.mqtt.MqttConnector;
import io.freedriver.autonomy.mqtt.MqttLog;
import io.freedriver.autonomy.mqtt.MqttStatePublisher;
import io.freedriver.autonomy.mqtt.PendingSource;
import io.freedriver.mqtt.contract.ApplianceCommandList;
import io.freedriver.mqtt.contract.PendingCommand;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Opens the scheduled store on startup and runs the list engine. Command-lists
 * subscribe and the scheduled publish stay off until the broker grants are live.
 * A firing still switches when the connector has not started; the acknowledgement
 * is skipped in that case.
 */
@ApplicationScoped
public class ScheduledCommands implements CommandListSink, PendingSource {

    private static final Logger LOG = LoggerFactory.getLogger(MqttLog.CATEGORY);

    private final String storeDir;
    private final String commandsEnabled;
    private final Instance<MqttStatePublisher> publishers;
    private final AliasBoardGateway boards;
    private final io.freedriver.autonomy.mqtt.CacheApplianceStateSource states;

    private final AtomicBoolean logged = new AtomicBoolean();
    private ScheduledExecutorService timers;
    private H2PendingStore store;
    private CommandListEngine engine;

    @Inject
    public ScheduledCommands(
            @ConfigProperty(name = "autonomy.scheduled.store-dir") String storeDir,
            @ConfigProperty(name = "autonomy.mqtt.commands-enabled") String commandsEnabled,
            Instance<MqttStatePublisher> publishers,
            AliasBoardGateway boards,
            io.freedriver.autonomy.mqtt.CacheApplianceStateSource states) {
        this.storeDir = storeDir;
        this.commandsEnabled = commandsEnabled;
        this.publishers = publishers;
        this.boards = boards;
        this.states = states;
    }

    void onStart(@Observes @Priority(1) StartupEvent event) {
        timers = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "command-list-timers");
            thread.setDaemon(true);
            return thread;
        });
        Optional<H2PendingStore> opened = H2PendingStore.open(Path.of(storeDir));
        if (opened.isEmpty()) {
            if (logged.compareAndSet(false, true)) {
                LOG.info("command-lists not subscribed reason=scheduled store unavailable");
            }
            return;
        }
        store = opened.get();
        engine = new CommandListEngine(
                store,
                this::boardFor,
                boards,
                this::arm,
                Instant::now,
                this::commandsOn,
                this::acknowledge);
        engine.recover();
    }

    void onStop(@Observes ShutdownEvent event) {
        if (timers != null) {
            timers.shutdownNow();
        }
        if (store != null) {
            store.close();
        }
    }

    public boolean available() {
        return engine != null;
    }

    @Override
    public CommandListResult accept(ApplianceCommandList list, Instant receivedAt) {
        CommandListEngine current = engine;
        if (current == null) {
            return new CommandListResult.Dropped("store unavailable");
        }
        return current.accept(list, receivedAt);
    }

    @Override
    public List<PendingCommand> pending() {
        CommandListEngine current = engine;
        if (current == null) {
            return List.of();
        }
        return current.pending();
    }

    private Optional<UUID> boardFor(String name) {
        try {
            return states.boardFor(name);
        } catch (Exception e) {
            LOG.info("command list skipped target={} reason=unknown appliance", name);
            return Optional.empty();
        }
    }

    private boolean commandsOn() {
        return "true".equals(commandsEnabled == null ? "" : commandsEnabled.trim());
    }

    private void arm(UUID id, Instant dueAt, Instant when) {
        long delay = Math.max(0, Duration.between(Instant.now(), when).toMillis());
        timers.schedule(() -> engine.onFire(id, dueAt), delay, TimeUnit.MILLISECONDS);
    }

    private void acknowledge(UUID id) {
        if (!publishers.isResolvable()) {
            return;
        }
        MqttConnector connector = publishers.get().connector();
        if (connector == null) {
            return;
        }
        connector.scheduledListener().fired(id);
    }
}
