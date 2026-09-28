package io.freedriver.autonomy.mqtt.impl;

/**
 * A password or CA file could not be used. The message is a fixed cause name,
 * never file contents.
 */
final class ConfigFileException extends Exception {

    ConfigFileException(String causeName) {
        super(causeName);
    }
}
