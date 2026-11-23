package io.freedriver.autonomy.mqtt;

import java.util.List;

import io.freedriver.mqtt.contract.PendingCommand;

/**
 * Complete pending set for a scheduled snapshot. The list order is the
 * snapshot order. A runtime exception means this publish is skipped.
 */
public interface PendingSource {

    List<PendingCommand> pending();
}
