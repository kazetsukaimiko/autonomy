package io.freedriver.autonomy.service;

import java.text.MessageFormat;
import java.util.logging.LogRecord;
import java.util.regex.Matcher;

final class LogMessages {

    private LogMessages() {
    }

    static String format(LogRecord record) {
        String message = record.getMessage();
        Object[] parameters = record.getParameters();
        if (message == null || parameters == null || parameters.length == 0) {
            return message;
        }
        try {
            if (message.contains("{0}")) {
                return MessageFormat.format(message, parameters);
            }
            if (message.contains("{}")) {
                String formatted = message;
                for (Object parameter : parameters) {
                    formatted = formatted.replaceFirst("\\{}", Matcher.quoteReplacement(String.valueOf(parameter)));
                }
                return formatted;
            }
        } catch (RuntimeException ignored) {
            return message;
        }
        return message;
    }
}
