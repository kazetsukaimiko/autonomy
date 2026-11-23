package io.freedriver.autonomy.scheduled;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import io.freedriver.autonomy.mqtt.CommandListResult;
import io.freedriver.autonomy.mqtt.MqttLog;
import io.freedriver.mqtt.contract.ApplianceCommandEntry;
import io.freedriver.mqtt.contract.ApplianceCommandList;
import io.freedriver.mqtt.contract.ApplianceSchemas;
import io.freedriver.mqtt.contract.CancelCommandEntry;
import io.freedriver.mqtt.contract.CommandListEntry;
import io.freedriver.mqtt.contract.PendingCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Admits, cancels, and fires command lists on one lock. No interlock keeps the
 * water inlet and the water tank from running together, so an on for either
 * name is skipped. Offs for them are accepted. The hakobune inlet step stays off.
 */
public final class CommandListEngine {

    static final Duration STALE = Duration.ofSeconds(20);
    static final Set<String> WATER_ON_HELD = Set.of("water_inlet", "water_tank");

    private static final Logger LOG = LoggerFactory.getLogger(MqttLog.CATEGORY);

    private final PendingStore store;
    private final Targets targets;
    private final BoardGateway boards;
    private final Alarms alarms;
    private final Clock clock;
    private final BooleanSupplier commandsEnabled;
    private final AppliedIds appliedIds;

    public CommandListEngine(PendingStore store, Targets targets, BoardGateway boards, Alarms alarms, Clock clock,
            BooleanSupplier commandsEnabled, AppliedIds appliedIds) {
        this.store = store;
        this.targets = targets;
        this.boards = boards;
        this.alarms = alarms;
        this.clock = clock;
        this.commandsEnabled = commandsEnabled;
        this.appliedIds = appliedIds;
    }

    public synchronized List<PendingCommand> pending() {
        return store.pending().stream()
                .map(row -> new PendingCommand(row.id().toString(), row.target(), row.on(), row.dueAt()))
                .toList();
    }

    public synchronized void recover() {
        if (!commandsEnabled.getAsBoolean()) {
            return;
        }
        Instant now = clock.now();
        for (PendingRow row : store.pending()) {
            if (row.dueAt().isAfter(now)) {
                arm(row);
            } else if (!row.on()) {
                fire(row.id(), row.dueAt());
            } else {
                store.delete(row.id());
                LOG.info("command list dropped target={} reason=overdue on", row.target());
            }
        }
    }

    public synchronized CommandListResult accept(ApplianceCommandList list, Instant receivedAt) {
        if (list == null || list.commands() == null || list.commands().isEmpty() || list.issuedAt() == null
                || receivedAt == null) {
            return new CommandListResult.Dropped("malformed");
        }
        UUID listId = canonical(list.listId());
        if (listId == null) {
            return new CommandListResult.Dropped("non-canonical id");
        }
        Duration skew = Duration.between(list.issuedAt(), receivedAt).abs();
        if (skew.compareTo(STALE) > 0) {
            LOG.info("command list dropped listId={} reason=stale issuedAt", list.listId());
            return new CommandListResult.Dropped("stale issuedAt");
        }
        if (store.seen(listId, receivedAt)) {
            LOG.info("command list dropped listId={} reason=replayed listId", list.listId());
            return new CommandListResult.Dropped("replayed listId");
        }
        if (!commandsEnabled.getAsBoolean()) {
            LOG.info("command list dropped listId={} reason=commands disabled", list.listId());
            return new CommandListResult.Dropped("commands disabled");
        }
        List<Parsed> parsed = new ArrayList<>();
        for (CommandListEntry entry : list.commands()) {
            UUID entryId = canonical(entry.id());
            if (entryId == null || entryId.equals(listId)) {
                return new CommandListResult.Dropped("non-canonical id");
            }
            if (entry instanceof CancelCommandEntry cancel) {
                if ((cancel.target() == null) == (cancel.cancelId() == null)) {
                    return new CommandListResult.Dropped("malformed");
                }
                if (cancel.cancelId() != null && canonical(cancel.cancelId()) == null) {
                    return new CommandListResult.Dropped("non-canonical id");
                }
                parsed.add(new Parsed(entryId, cancel, null));
            } else if (entry instanceof ApplianceCommandEntry command) {
                parsed.add(new Parsed(entryId, null, command));
            } else {
                return new CommandListResult.Dropped("malformed");
            }
        }
        Map<UUID, PendingRow> copy = new LinkedHashMap<>();
        for (PendingRow row : store.pending()) {
            copy.put(row.id(), row);
        }
        Set<UUID> cancelIds = new LinkedHashSet<>();
        Set<String> cancelTargets = new LinkedHashSet<>();
        List<PendingRow> added = new ArrayList<>();
        List<List<Switch>> runs = new ArrayList<>();
        List<Switch> run = new ArrayList<>();
        for (Parsed item : parsed) {
            if (item.cancel() != null) {
                flush(runs, run);
                CancelCommandEntry cancel = item.cancel();
                if (cancel.cancelId() != null) {
                    UUID id = UUID.fromString(cancel.cancelId());
                    copy.remove(id);
                    cancelIds.add(id);
                } else {
                    copy.values().removeIf(row -> cancel.target().equals(row.target()));
                    cancelTargets.add(cancel.target());
                }
                continue;
            }
            ApplianceCommandEntry command = item.command();
            Optional<UUID> board = targets.boardFor(command.target());
            if (board.isEmpty()) {
                LOG.info("command list skipped target={} reason=unknown appliance", command.target());
                continue;
            }
            boolean on = Boolean.TRUE.equals(command.state());
            if (on && WATER_ON_HELD.contains(command.target())) {
                LOG.info("command list skipped target={} reason=water on held", command.target());
                continue;
            }
            Duration delay = command.delay();
            boolean delayed = delay != null && delay.compareTo(Duration.ZERO) > 0;
            if (!delayed) {
                run.add(new Switch(board.get(), command.target(), on));
                continue;
            }
            flush(runs, run);
            if (copy.containsKey(item.id())) {
                LOG.info("command list refused listId={} reason=pending id", list.listId());
                return new CommandListResult.Refused("pending id");
            }
            if (copy.size() >= ApplianceSchemas.MAX_PENDING) {
                LOG.info("command list refused listId={} reason=pending full", list.listId());
                return new CommandListResult.Refused("pending full");
            }
            PendingRow row = new PendingRow(item.id(), command.target(), on, receivedAt.plus(delay));
            copy.put(row.id(), row);
            added.add(row);
        }
        flush(runs, run);
        try {
            store.admit(listId, receivedAt, cancelIds, cancelTargets, added);
        } catch (RuntimeException e) {
            LOG.info("command list dropped listId={} reason=store failed", list.listId());
            throw e;
        }
        boolean writeFailed = false;
        if (commandsEnabled.getAsBoolean()) {
            for (List<Switch> switches : runs) {
                Map<UUID, Map<String, Boolean>> byBoard = new LinkedHashMap<>();
                for (Switch sw : switches) {
                    byBoard.computeIfAbsent(sw.board(), ignored -> new LinkedHashMap<>()).put(sw.target(), sw.on());
                }
                for (Map.Entry<UUID, Map<String, Boolean>> write : byBoard.entrySet()) {
                    if (boards.write(write.getKey(), write.getValue()) != WriteResult.APPLIED) {
                        writeFailed = true;
                        LOG.info("command list write failed listId={} board={}", list.listId(), write.getKey());
                    }
                }
            }
            for (PendingRow row : added) {
                if (row.dueAt().isAfter(clock.now())) {
                    arm(row);
                } else if (!row.on()) {
                    fire(row.id(), row.dueAt());
                } else {
                    store.delete(row.id());
                    LOG.info("command list dropped target={} reason=overdue on", row.target());
                }
            }
        }
        if (writeFailed) {
            return new CommandListResult.Dropped("undelayed write failed");
        }
        return new CommandListResult.Applied(list.listId());
    }

    public synchronized void onFire(UUID id, Instant dueAt) {
        fire(id, dueAt);
    }

    private void fire(UUID id, Instant dueAt) {
        if (!commandsEnabled.getAsBoolean()) {
            return;
        }
        Optional<PendingRow> found = store.find(id);
        if (found.isEmpty() || !found.get().dueAt().equals(dueAt)) {
            return;
        }
        PendingRow row = found.get();
        Optional<UUID> board = targets.boardFor(row.target());
        if (board.isEmpty()) {
            LOG.info("command list skipped target={} reason=unknown appliance", row.target());
            return;
        }
        WriteResult result = boards.write(board.get(), Map.of(row.target(), row.on()));
        if (result == WriteResult.APPLIED) {
            store.delete(row.id());
            appliedIds.applied(row.id());
            return;
        }
        if (!row.on()) {
            alarms.arm(row.id(), row.dueAt(), clock.now().plusSeconds(10));
            return;
        }
        store.delete(row.id());
        LOG.info("command list dropped target={} reason=on write failed", row.target());
    }

    private void arm(PendingRow row) {
        alarms.arm(row.id(), row.dueAt(), row.dueAt());
    }

    private static void flush(List<List<Switch>> runs, List<Switch> run) {
        if (!run.isEmpty()) {
            runs.add(List.copyOf(run));
            run.clear();
        }
    }

    static UUID canonical(String id) {
        if (id == null || !id.equals(id.toLowerCase(Locale.ROOT))) {
            return null;
        }
        try {
            UUID parsed = UUID.fromString(id);
            if (!parsed.toString().equals(id)) {
                return null;
            }
            return parsed;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public interface Targets {
        Optional<UUID> boardFor(String name);
    }

    public interface BoardGateway {
        WriteResult write(UUID board, Map<String, Boolean> desired);
    }

    public enum WriteResult {
        APPLIED,
        FAILED,
        UNCONFIRMED
    }

    public interface Alarms {
        void arm(UUID id, Instant dueAt, Instant when);
    }

    public interface Clock {
        Instant now();
    }

    public interface AppliedIds {
        void applied(UUID id);
    }

    private record Parsed(UUID id, CancelCommandEntry cancel, ApplianceCommandEntry command) {
    }

    private record Switch(UUID board, String target, boolean on) {
    }
}
