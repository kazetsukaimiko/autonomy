package io.freedriver.autonomy.mqtt;

import java.util.UUID;

/**
 * A delayed entry has fired and its switch has returned. The connector
 * enqueues {@code entryId} as {@code appliedCommandId} and publishes the
 * scheduled snapshot.
 */
public interface ScheduledListener {

    void fired(UUID entryId);
}
