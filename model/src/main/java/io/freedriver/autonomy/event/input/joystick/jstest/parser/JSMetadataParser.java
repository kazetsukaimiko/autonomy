package io.freedriver.autonomy.event.input.joystick.jstest.parser;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.freedriver.autonomy.event.input.joystick.jstest.JSMetadata;

/**
 * Parses a jstest header line into updated {@link JSMetadata}. Empty if the line is an event line.
 */
public final class JSMetadataParser implements BiFunction<JSMetadata, String, Optional<JSMetadata>> {
    public static final JSMetadataParser INSTANCE = new JSMetadataParser();

    private static final String HW_TYPE = "hwtype";
    private static final String TITLE = "title";
    private static final String NUM_AXES = "numaxes";
    private static final String AXIS_NAMES = "axisnames";
    private static final String NUM_BUTTONS = "numbuttons";
    private static final String BUTTON_NAMES = "buttonnames";
    private static final String DRIVERVER = "driverver";

    private static final Pattern DETAILS = Pattern.compile(
            "(?<" + HW_TYPE + ">[a-zA-Z]+)\\s+\\((?<" + TITLE + ">[a-zA-Z\\s\\d+\\.]+)\\)\\s+has\\s+(?<"
                    + NUM_AXES + ">\\d+)\\s+axes\\s+\\((?<" + AXIS_NAMES + ">[a-zA-Z\\d\\s,]+)\\)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern DESCRIPTION = Pattern.compile(
            "and\\s+(?<" + NUM_BUTTONS + ">\\d+) buttons \\((?<" + BUTTON_NAMES + ">[a-zA-Z\\d\\s+,]+)\\)\\.",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern DRIVER_VERSION = Pattern.compile(
            "Driver version is (?<" + DRIVERVER + ">[\\d\\.]+)\\.", Pattern.CASE_INSENSITIVE);

    @Override
    public Optional<JSMetadata> apply(JSMetadata current, String line) {
        if (JSTestEventParser.isEventLine(line)) {
            return Optional.empty();
        }
        Matcher detailsMatcher = DETAILS.matcher(line);
        if (detailsMatcher.matches()) {
            return Optional.of(current.toBuilder()
                    .title(detailsMatcher.group(TITLE))
                    .hardwareType(detailsMatcher.group(HW_TYPE))
                    .axisNames(index(detailsMatcher.group(AXIS_NAMES), current.axisNames()))
                    .build());
        }
        Matcher descriptionMatcher = DESCRIPTION.matcher(line);
        if (descriptionMatcher.matches()) {
            return Optional.of(current.toBuilder()
                    .buttonNames(index(descriptionMatcher.group(BUTTON_NAMES), current.buttonNames()))
                    .build());
        }
        Matcher driverMatcher = DRIVER_VERSION.matcher(line);
        if (driverMatcher.matches()) {
            return Optional.of(current.toBuilder()
                    .driverVersion(driverMatcher.group(DRIVERVER))
                    .build());
        }
        return Optional.of(current);
    }

    static Map<Integer, String> index(String source, Map<Integer, String> existing) {
        Map<Integer, String> names = new HashMap<>(existing);
        String[] parts = source.split(",\\s*");
        for (int i = 0; i < parts.length; i++) {
            names.put(i, parts[i]);
        }
        return names;
    }
}
