package io.freedriver.autonomy.mqtt;

import java.time.Instant;

import io.freedriver.mqtt.contract.ApplianceCommandList;

/**
 * Admits one command list that already passed the topic, retain, size, and
 * parse checks. A runtime exception means nothing was stored and the connector
 * enqueues no list id.
 */
public interface CommandListSink {

    /**
     * @param receivedAt when the connector received the payload
     */
    CommandListResult accept(ApplianceCommandList list, Instant receivedAt);
}
