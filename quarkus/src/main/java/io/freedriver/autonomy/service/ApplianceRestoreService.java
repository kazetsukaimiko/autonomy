package io.freedriver.autonomy.service;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import io.freedriver.jsonlink.config.v2.Appliance;
import io.freedriver.jsonlink.config.v2.Mapping;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Puts each appliance back to its saved on or off state after the board finishes
 * its reset and the UUID handshake succeeds.
 *
 * <p>At most one restore runs per {@code autonomy.state.restore-window}. A reconnect
 * inside that window is logged and does not change outputs. Board-reported changes
 * are ignored from the moment the port is open until that restore attempt finishes,
 * so the all-off state after reset is not saved over the previous state.
 */
@ApplicationScoped
@Slf4j
public class ApplianceRestoreService {

    private final ApplianceStateStore store;
    private final Clock clock;
    private final Duration window;
    private final Set<UUID> awaitingRestore = ConcurrentHashMap.newKeySet();
    private Instant lastRestore;

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
     * Restores every newly connected board, or logs and skips them when a restore
     * already ran inside the window.
     */
    public synchronized void onBoardsReady(Collection<UUID> boardIds) {
        if (boardIds.isEmpty()) {
            return;
        }
        if (insideRestoreWindow()) {
            for (UUID boardId : boardIds) {
                log.info("Board {} reconnected inside the restore window; not restoring again", boardId);
            }
            return;
        }
        LoadedApplianceState loaded = store.load();
        if (loaded.status() != LoadedApplianceState.Status.PRESENT) {
            log.info("No saved appliance state exists; leaving every appliance off");
            boardIds.forEach(awaitingRestore::remove);
            return;
        }
        boolean restored = false;
        for (UUID boardId : boardIds) {
            if (restoreBoard(boardId, loaded.states())) {
                restored = true;
            }
        }
        if (restored) {
            lastRestore = clock.instant();
        }
    }

    private boolean insideRestoreWindow() {
        if (lastRestore == null) {
            return false;
        }
        return Duration.between(lastRestore, clock.instant()).compareTo(window) < 0;
    }

    private boolean restoreBoard(UUID boardId, Map<String, Boolean> saved) {
        Mapping mapping;
        try {
            mapping = aliases.getMapping(boardId);
        } catch (IOException | RuntimeException e) {
            log.warn("Couldn't restore appliances for board {}", boardId, e);
            return false;
        }
        for (String name : saved.keySet()) {
            boolean known = mapping.appliances().stream()
                    .anyMatch(appliance -> Objects.equals(name, appliance.name()));
            if (!known) {
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
}
