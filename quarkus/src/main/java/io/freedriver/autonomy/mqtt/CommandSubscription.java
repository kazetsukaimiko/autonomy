package io.freedriver.autonomy.mqtt;

/**
 * Whether this process subscribes to commands. Only the value {@code true}
 * subscribes, and only when MQTT itself is on and the payload limit parses.
 * Any other commands-enabled value leaves the topic unsubscribed and does not
 * stop the process.
 */
final class CommandSubscription {

    private final boolean subscribe;
    private final int maxBytes;
    private final String reason;

    private CommandSubscription(boolean subscribe, int maxBytes, String reason) {
        this.subscribe = subscribe;
        this.maxBytes = maxBytes;
        this.reason = reason;
    }

    boolean subscribe() {
        return subscribe;
    }

    int maxBytes() {
        return maxBytes;
    }

    String reason() {
        return reason;
    }

    static CommandSubscription decide(String commandsEnabled, String maxBytes, boolean mqttOn) {
        if (!"true".equals(commandsEnabled == null ? "" : commandsEnabled.trim())) {
            return off("autonomy.mqtt.commands-enabled is not true");
        }
        if (!mqttOn) {
            return off("autonomy.mqtt.enabled is not true");
        }
        Integer limit = parseLimit(maxBytes);
        if (limit == null) {
            return off("autonomy.mqtt.command-max-bytes is invalid");
        }
        return new CommandSubscription(true, limit, null);
    }

    private static CommandSubscription off(String reason) {
        return new CommandSubscription(false, 4096, reason);
    }

    static Integer parseLimit(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            if (value < 1) {
                return null;
            }
            return value;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
