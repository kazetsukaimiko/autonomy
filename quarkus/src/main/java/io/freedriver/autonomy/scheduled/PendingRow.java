package io.freedriver.autonomy.scheduled;

import java.time.Instant;
import java.util.UUID;

/**
 * One delayed command stored by entry id.
 */
public record PendingRow(UUID id, String target, boolean on, Instant dueAt) {
}
