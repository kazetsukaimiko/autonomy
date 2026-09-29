package io.freedriver.autonomy.mqtt;

import java.time.Duration;

import io.quarkus.runtime.configuration.DurationConverter;

/**
 * Parses {@code autonomy.mqtt} text the same way Quarkus used to, after startup.
 */
final class MqttConfigValues {

    private MqttConfigValues() {
    }

    static Boolean enabled(String raw) {
        if (raw == null) {
            return null;
        }
        String text = raw.trim();
        if (text.equalsIgnoreCase("true")) {
            return Boolean.TRUE;
        }
        if (text.equalsIgnoreCase("false")) {
            return Boolean.FALSE;
        }
        return null;
    }

    static Integer port(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            int port = Integer.parseInt(raw.trim());
            if (port < 1 || port > 65535) {
                return null;
            }
            return port;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static Duration duration(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            Duration parsed = DurationConverter.parseDuration(raw);
            if (parsed == null || parsed.isZero() || parsed.isNegative()) {
                return null;
            }
            return parsed;
        } catch (RuntimeException e) {
            return null;
        }
    }
}
