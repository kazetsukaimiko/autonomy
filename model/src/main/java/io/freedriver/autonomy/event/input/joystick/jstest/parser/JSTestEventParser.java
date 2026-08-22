package io.freedriver.autonomy.event.input.joystick.jstest.parser;

import java.time.Instant;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.freedriver.autonomy.event.input.joystick.jstest.JSMetadata;
import io.freedriver.autonomy.event.input.joystick.jstest.JSTestEvent;
import io.freedriver.autonomy.event.input.joystick.jstest.JSTestEventType;

/**
 * Parses a jstest {@code Event:} line into a {@link JSTestEvent} using the current device metadata.
 */
public final class JSTestEventParser implements Function<String, JSTestEvent> {

    private final JSMetadata metadata;

    public JSTestEventParser(JSMetadata metadata) {
        this.metadata = metadata;
    }

    public static boolean isEventLine(String line) {
        return line != null && line.startsWith("Event: ");
    }

    @Override
    public JSTestEvent apply(String jstestEvent) {
        Map<String, Long> fields = parseFields(jstestEvent);
        return new JSTestEvent(
                metadata,
                Instant.now(),
                JSTestEventType.ofTypeNumber(fields.get("type")),
                fields.get("time"),
                fields.get("number"),
                fields.get("value"));
    }

    private static Map<String, Long> parseFields(String jstestEvent) {
        return Stream.of(jstestEvent)
                .filter(JSTestEventParser::isEventLine)
                .map(eventLine -> eventLine.split("Event: "))
                .filter(eventLine -> eventLine.length == 2)
                .map(eventLine -> eventLine[1])
                .map(eventContent -> eventContent.split("\\s*,\\s* "))
                .filter(eventPairs -> eventPairs.length == 4)
                .flatMap(Stream::of)
                .map(kvpair -> kvpair.split("\\s+"))
                .collect(Collectors.toMap(kvpair -> kvpair[0], kvpair -> Long.parseLong(kvpair[1])));
    }
}
