package io.freedriver.autonomy.service.startup;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BooleanSupplier;

import io.freedriver.autonomy.service.ConnectorServiceCommon;
import io.freedriver.jsonlink.config.v2.MappingsAction;
import lombok.extern.slf4j.Slf4j;

/**
 * Runs one board's startup list under the board lock. A delay releases that lock
 * so REST and the joystick can send. The first failed action skips the rest.
 */
@Slf4j
public final class StartupActionRunner {
    private final ConnectorServiceCommon connectors;
    private final DelayWait wait;
    private final Failure failure;

    StartupActionRunner(ConnectorServiceCommon connectors, DelayWait wait, Failure failure) {
        this.connectors = connectors;
        this.wait = wait;
        this.failure = failure;
    }

    static DelayWait pollingWait() {
        return StartupActionRunner::poll;
    }

    void run(
            UUID boardId,
            List<MappingsAction> actions,
            GpioControl gpio,
            ApplianceControl appliances,
            BooleanSupplier connected) {
        if (actions.isEmpty()) {
            return;
        }
        connectors.withBoardLock(() -> {
            for (MappingsAction action : actions) {
                if (!connected.getAsBoolean()) {
                    log.info("Board {} disconnected; startupActions stopped", boardId);
                    return;
                }
                if (action instanceof DelayAction delay) {
                    boolean stayed;
                    try {
                        stayed = connectors.callWithoutBoardLock(
                                () -> wait.await(delay.getDuration(), connected));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        failure.failed(boardId, "startupActions interrupted during delay");
                        return;
                    } catch (RuntimeException e) {
                        failure.failed(boardId, "startupActions delay failed: " + e.getMessage());
                        return;
                    }
                    if (!stayed) {
                        log.info("Board {} disconnected during startup delay; list stopped", boardId);
                        return;
                    }
                    continue;
                }
                if (!(action instanceof AutonomyMappingsAction autonomyAction)) {
                    failure.failed(boardId, "startup action is not runnable");
                    return;
                }
                try {
                    autonomyAction.accept(gpio, appliances);
                } catch (RuntimeException e) {
                    failure.failed(boardId, "startupActions failed: " + e.getMessage());
                    return;
                }
            }
        });
    }

    /**
     * Waits out {@code duration}, returning false as soon as the board is gone.
     * Slices stay short so a disconnect does not wait out the whole delay.
     */
    public static boolean poll(Duration duration, BooleanSupplier stillConnected) throws InterruptedException {
        long end = System.nanoTime() + duration.toNanos();
        while (true) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            if (!stillConnected.getAsBoolean()) {
                return false;
            }
            long remaining = end - System.nanoTime();
            if (remaining <= 0) {
                return stillConnected.getAsBoolean();
            }
            LockSupport.parkNanos(Math.min(remaining, 50_000_000L));
        }
    }

    @FunctionalInterface
    public interface DelayWait {
        boolean await(Duration duration, BooleanSupplier stillConnected) throws InterruptedException;
    }

    @FunctionalInterface
    interface Failure {
        void failed(UUID boardId, String message);
    }
}
