package io.freedriver.autonomy.scheduled;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Pending rows and recently applied list ids. Implementations apply one
 * {@link #admit} call in a single transaction.
 */
public interface PendingStore {

    List<PendingRow> pending();

    Optional<PendingRow> find(UUID id);

    boolean seen(UUID listId, Instant now);

    void admit(UUID listId, Instant seenAt, Set<UUID> cancelIds, Set<String> cancelTargets, List<PendingRow> added);

    void delete(UUID id);

    /** Drops applied list ids seen strictly before {@code cutoff}. */
    void purgeSeenBefore(Instant cutoff);
}
