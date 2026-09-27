package io.freedriver.autonomy.service;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.freedriver.jsonlink.config.v2.Appliance;
import io.freedriver.jsonlink.config.v2.Mapping;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Puts each appliance back to its saved on or off state after the board finishes
 * its reset and the UUID handshake succeeds.
 *
 * <p>Each board is restored at most once per {@code autonomy.state.restore-window}.
 * A reconnect inside that board's window is logged and does not change outputs
 * immediately. One restore for that board is scheduled at the end of the window,
 * so the board does not stay in the holding state waiting for another reconnect.
 * A further reconnect inside the same window does not schedule a second restore.
 * Board-reported changes are ignored from the moment the port is open until that
 * restore attempt finishes, so the all-off state after reset is not saved over
 * the previous state.
 */
@ApplicationScoped
@Slf4j
public class ApplianceRestoreService {

    private final ApplianceStateStore store;
    private final Clock clock;
    private final Duration window;
    private final BoardRestoreScheduler scheduler;
    private final ScheduledExecutorService schedulerExecutor;
    private final Set<UUID> awaitingRestore = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Instant> lastRestoreByBoard = new HashMap<>();
    private final Map<UUID, Instant> deferredDue = new HashMap<>();
    private final Object restoreLock = new Object();

    @Inject
    SimpleAliasService aliases;

    @Inject
    public ApplianceRestoreService(
            ApplianceStateStore store,
            Clock clock,
            @ConfigProperty(name = "autonomy.state.restore-window", defaultValue = "30s") Duration window) {
        this.store = store;
        this.clock = clock;
        this.window = window;
        this.schedulerExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "appliance-restore");
            thread.setDaemon(true);
            return thread;
        });
        this.scheduler = (command, delay) ->
                schedulerExecutor.schedule(command, delay.toMillis(), TimeUnit.MILLISECONDS);
    }

    ApplianceRestoreService(
            ApplianceStateStore store,
            Clock clock,
            Duration window,
            BoardRestoreScheduler scheduler) {
        this.store = store;
        this.clock = clock;
        this.window = window;
        this.schedulerExecutor = null;
        this.scheduler = scheduler;
    }

    @PreDestroy
    void stopScheduler() {
        if (schedulerExecutor != null) {
            schedulerExecutor.shutdownNow();
        }
    }

    /**
     * The board has finished reset and its handshake succeeded, but saved outputs
     * have not been written back yet.
     */
    public void beginAwaitingRestore(UUID boardId) {
        awaitingRestore.add(boardId);
    }

    public boolean isAwaitingRestore(UUID boardId) {
        return awaitingRestore.contains(boardId);
    }

    /**
     * Restores every newly connected board whose own window has elapsed.
     * A board still inside its window keeps waiting until that window ends.
     */
    public void onBoardsReady(Collection<UUID> boardIds) {
        if (boardIds.isEmpty()) {
            return;
        }
        synchronized (restoreLock) {
            acceptBoards(boardIds);
        }
    }

    /**
     * Runs deferred restores whose window has elapsed. Tests advance the clock
     * and call this instead of waiting out the scheduled delay.
     */
    void runDueDeferredRestores() {
        List<UUID> dueBoards;
        synchronized (restoreLock) {
            Instant now = clock.instant();
            dueBoards = deferredDue.entrySet().stream()
                    .filter(entry -> !now.isBefore(entry.getValue()))
                    .map(Map.Entry::getKey)
                    .toList();
        }
        for (UUID boardId : dueBoards) {
            runDeferredRestore(boardId);
        }
    }

    int pendingDeferredRestores() {
        synchronized (restoreLock) {
            return deferredDue.size();
        }
    }

    private void acceptBoards(Collection<UUID> boardIds) {
        List<UUID> ready = new ArrayList<>();
        for (UUID boardId : boardIds) {
            if (insideWindow(boardId)) {
                log.info("Board {} reconnected inside the restore window; not restoring again", boardId);
                scheduleDeferredRestore(boardId);
                continue;
            }
            deferredDue.remove(boardId);
            ready.add(boardId);
        }
        if (!ready.isEmpty()) {
            restoreUnlocked(ready);
        }
    }

    private void scheduleDeferredRestore(UUID boardId) {
        Instant due = lastRestoreByBoard.get(boardId).plus(window);
        if (deferredDue.putIfAbsent(boardId, due) != null) {
            return;
        }
        Duration delay = Duration.between(clock.instant(), due);
        if (delay.isNegative()) {
            delay = Duration.ZERO;
        }
        scheduler.schedule(() -> {
            try {
                runDeferredRestore(boardId);
            } catch (RuntimeException e) {
                log.warn("Couldn't run the scheduled appliance restore for board {}", boardId, e);
            }
        }, delay);
    }

    private void runDeferredRestore(UUID boardId) {
        synchronized (restoreLock) {
            if (deferredDue.remove(boardId) == null) {
                return;
            }
            restoreUnlocked(List.of(boardId));
        }
    }

    private void restoreUnlocked(Collection<UUID> boardIds) {
        aliases.withBoardLock(() -> {
            LoadedApplianceState loaded = store.load();
            if (loaded.status() != LoadedApplianceState.Status.PRESENT) {
                log.info("No saved appliance state exists; leaving every appliance off");
                boardIds.forEach(awaitingRestore::remove);
                return;
            }
            for (UUID boardId : boardIds) {
                if (restoreBoard(boardId, loaded.states())) {
                    lastRestoreByBoard.put(boardId, clock.instant());
                }
            }
        });
    }

    private boolean insideWindow(UUID boardId) {
        Instant last = lastRestoreByBoard.get(boardId);
        if (last == null) {
            return false;
        }
        return Duration.between(last, clock.instant()).compareTo(window) < 0;
    }

    private boolean restoreBoard(UUID boardId, Map<String, Boolean> saved) {
        Mapping mapping;
        Set<String> knownNames;
        try {
            mapping = aliases.getMapping(boardId);
            knownNames = applianceNames();
        } catch (IOException | RuntimeException e) {
            log.warn("Couldn't restore appliances for board {}", boardId, e);
            return false;
        }
        for (String name : saved.keySet()) {
            boolean onThisBoard = mapping.appliances().stream()
                    .anyMatch(appliance -> Objects.equals(name, appliance.name()));
            if (!onThisBoard && !knownNames.contains(name)) {
                log.warn("Skipping saved state for unknown appliance '{}'", name);
            }
        }
        Map<String, Boolean> desired = new LinkedHashMap<>();
        for (Appliance appliance : mapping.appliances()) {
            desired.put(appliance.name(), Boolean.TRUE.equals(saved.get(appliance.name())));
        }
        try {
            if (!desired.isEmpty()) {
                aliases.setState(boardId, desired);
            }
        } catch (BoardNotFoundException unplugged) {
            log.info("Board {} is unplugged; the next reconnect will restore it", boardId);
            lastRestoreByBoard.remove(boardId);
            return false;
        } catch (IOException | RuntimeException e) {
            log.warn("Couldn't restore appliances for board {}", boardId, e);
            return false;
        }
        for (Appliance appliance : mapping.appliances()) {
            boolean on = Boolean.TRUE.equals(desired.get(appliance.name()));
            log.info("Restored appliance {} to {}", appliance.name(), on ? "ON" : "OFF");
        }
        awaitingRestore.remove(boardId);
        return true;
    }

    private Set<String> applianceNames() throws IOException {
        Set<String> names = new HashSet<>();
        for (Mapping mapping : aliases.getMappings().getMappings()) {
            for (Appliance appliance : mapping.appliances()) {
                names.add(appliance.name());
            }
        }
        return names;
    }

    @FunctionalInterface
    interface BoardRestoreScheduler {
        void schedule(Runnable command, Duration delay);
    }
}
