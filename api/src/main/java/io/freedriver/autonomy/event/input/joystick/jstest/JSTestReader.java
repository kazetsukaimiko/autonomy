package io.freedriver.autonomy.event.input.joystick.jstest;

import io.freedriver.autonomy.jpa.entity.event.input.joystick.jstest.JSMetadata;
import io.freedriver.autonomy.jpa.entity.event.input.joystick.jstest.JSTestEvent;
import io.freedriver.util.ProcessUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JSTestReader {
    private static final Logger LOGGER = Logger.getLogger(JSTestReader.class.getName());

    private static final String HW_TYPE = "hwtype";
    private static final String TITLE = "title";
    private static final String NUM_AXES = "numaxes";
    private static final String AXIS_NAMES = "axisnames";

    private static final String NUM_BUTTONS = "numbuttons";
    private static final String BUTTON_NAMES = "buttonnames";

    private static final String DRIVERVER = "driverver";

    private static final Pattern DETAILS = Pattern.compile("(?<"+HW_TYPE+">[a-zA-Z]+)\\s+\\((?<"+TITLE+">[a-zA-Z\\s\\d+\\.]+)\\)\\s+has\\s+(?<"+NUM_AXES+">\\d+)\\s+axes\\s+\\((?<"+AXIS_NAMES+">[a-zA-Z\\d\\s,]+)\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern DESCRIPTION = Pattern.compile("and\\s+(?<"+NUM_BUTTONS+">\\d+) buttons \\((?<"+BUTTON_NAMES+">[a-zA-Z\\d\\s+,]+)\\)\\.", Pattern.CASE_INSENSITIVE);
    private static final Pattern DRIVER_VERSION = Pattern.compile("Driver version is (?<"+DRIVERVER+">[\\d\\.]+)\\.", Pattern.CASE_INSENSITIVE);


    private JSTestReader() {
    }

    /**
     * Take a stream of input lines, and transform them into JSTestEvents. Populate the JSMetadata object
     * as you find information for it.
     * @param rawLines The raw lines from jstest.
     * @return A Stream of populated JSTestEvent objects
     */
    public static Stream<JSTestEvent> readEvents(Stream<String> rawLines) {
        JSMetadata jsMetadata = new JSMetadata();
        return rawLines
                .flatMap(rawLine -> addOrMakeMetadata(jsMetadata, rawLine));
    }

    public static Stream<JSTestEvent> ofJoystick(Path joystickPath) {
        final Process p = jstestProcess(joystickPath);
        return readEvents(ProcessUtil.linesInputStream(p.getInputStream()).onClose(p::destroy));
    }

    public static List<Path> getJoysticksPaths() {
        return Stream.of(Paths.get("/dev/input/"))
                .map(Path::toFile)
                .map(File::listFiles)
                .flatMap(Stream::of)
                .filter(file -> file.getName().matches("js\\d+"))
                .filter(File::canRead)
                .map(File::toPath)
                .collect(Collectors.toList());
    }


    /*
     * METHODS - internal
     */

    /**
     * Spawns the jstest process.
     */
    private static Process jstestProcess(Path joystickPath) {
        try {
            return new ProcessBuilder(
                    "jstest",
                    "--event",
                    joystickPath.toAbsolutePath().toString())
                    .start();
        } catch (IOException e) {
            throw new JSTestException("Couldn't spawn jstest process: ", e);
        }
    }

    /**
     * For a given event line, detect if the line contains metadata, or event data.
     * If metadata, populate the JSMetadata object.
     * If event data, convert and add it to the Stream.
     * @return A Stream containing event data, or an empty stream.
     */
    private static Stream<JSTestEvent> addOrMakeMetadata(JSMetadata jsMetadata, String event) {
        if (!makeMetadata(jsMetadata, event)) {
            return Stream.of(new JSTestEvent(jsMetadata, event));
        }
        return Stream.empty();
    }

    /**
     * Side-effect loads details into the passed JSMetadata object, if they match metadata patterns.
     * Returns true if the event is metadata, false otherwise.
     */
    private static boolean makeMetadata(JSMetadata jsMetadata, String event) {
        if (!JSTestEvent.validEvent(event)) {
            if (jsMetadata == null) {
                jsMetadata = new JSMetadata();
            }
            Matcher detailsMatcher = DETAILS.matcher(event);
            Matcher descriptionMatcher = DESCRIPTION.matcher(event);
            Matcher driverMatcher = DRIVER_VERSION.matcher(event);
            if (detailsMatcher.matches()) {
                jsMetadata.setTitle(detailsMatcher.group(TITLE));
                jsMetadata.setHardwareType(detailsMatcher.group(HW_TYPE));
                JSMetadata.index(detailsMatcher.group(AXIS_NAMES), jsMetadata.getAxisNames()::put);
            } else if (descriptionMatcher.matches()) {
                JSMetadata.index(descriptionMatcher.group(BUTTON_NAMES), jsMetadata.getButtonNames()::put);
            } else if (driverMatcher.matches()) {
                jsMetadata.setDriverVersion(driverMatcher.group(DRIVERVER));
            } else {
                LOGGER.warning("Not details:");
                LOGGER.log(Level.WARNING, event);
            }

            return true;
        }
        return false;
    }
}
