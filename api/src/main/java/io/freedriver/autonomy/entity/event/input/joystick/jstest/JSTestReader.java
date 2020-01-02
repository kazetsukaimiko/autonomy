package io.freedriver.autonomy.entity.event.input.joystick.jstest;

import io.freedriver.util.ProcessUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Supplier;
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

    private static final Pattern DETAILS = Pattern.compile("(?<"+HW_TYPE+">[a-zA-Z]+)\\s+\\((?<"+TITLE+">[a-zA-Z\\s\\d+]+)\\)\\s+has\\s+(?<"+NUM_AXES+">\\d+)\\s+axes\\s+\\((?<"+AXIS_NAMES+">[a-zA-Z\\d\\s,]+)\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern DESCRIPTION = Pattern.compile("and\\s+(?<"+NUM_BUTTONS+">\\d+) buttons \\((?<"+BUTTON_NAMES+">[a-zA-Z\\d\\s+,]+)\\)\\.", Pattern.CASE_INSENSITIVE);

    public static Stream<JSTestEvent> reduce(Stream<String> rawLines) {
        JSMetadata jsMetadata = new JSMetadata();
        return rawLines
                .reduce(Stream.empty(), (a, s) -> Stream.concat(a, addOrMakeMetadata(jsMetadata, s)), Stream::concat);
    }

    private static Stream<JSTestEvent> addOrMakeMetadata(JSMetadata jsMetadata, String event) {
        if (!makeMetadata(jsMetadata, event)) {
            return Stream.of(new JSTestEvent(jsMetadata, event));
        }
        return Stream.empty();
    }

    public static boolean makeMetadata(JSMetadata jsMetadata, String event) {
        if (!JSTestEvent.validEvent(event)) {
            if (jsMetadata == null) {
                jsMetadata = new JSMetadata();
            }
            Matcher detailsMatcher = DETAILS.matcher(event);
            Matcher descriptionMatcher = DESCRIPTION.matcher(event);
            if (detailsMatcher.matches()) {
                jsMetadata.setTitle(detailsMatcher.group(TITLE));
                jsMetadata.setHardwareType(detailsMatcher.group(HW_TYPE));
                JSMetadata.index(detailsMatcher.group(AXIS_NAMES), jsMetadata.getAxisNames()::put);
            } else {
                LOGGER.warning("Not details:");
                LOGGER.log(Level.WARNING, event);
            }
            if (descriptionMatcher.matches()) {
                JSMetadata.index(descriptionMatcher.group(BUTTON_NAMES), jsMetadata.getButtonNames()::put);
            }
            return true;
        }
        return false;
    }

    private JSTestReader() {
    }

    public static Process jstestProcess(Path joystickPath) {
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

    public static Stream<JSTestEvent> ofJoystick(Path joystickPath) {
        final Process p = jstestProcess(joystickPath);
        return reduce(ProcessUtil.linesInputStream(p.getInputStream()).onClose(p::destroy));
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
}
