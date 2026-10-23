package io.freedriver.autonomy.mqtt;

import java.io.IOException;

/**
 * Applies one command that already passed the topic, retain, size, and parse
 * checks. Return false when the name is not in the mappings. Do not touch a
 * board in that case.
 */
public interface ApplianceCommandSink {

    boolean apply(String applianceName, boolean on) throws IOException;
}
