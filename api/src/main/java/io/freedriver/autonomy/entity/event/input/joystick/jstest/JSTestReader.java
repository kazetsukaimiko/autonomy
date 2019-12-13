package io.freedriver.autonomy.entity.event.input.joystick.jstest;

import io.freedriver.util.ProcessUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JSTestReader implements AutoCloseable {
    private static final String HW_TYPE = "hwtype";
    private static final String TITLE = "type";
    private static final String NUM_AXES = "numaxes";
    private static final String AXIS_NAMES = "axisnames";

    private static final String NUM_BUTTONS = "numbuttons";
    private static final String BUTTON_NAMES = "buttonnames";

    private static final Pattern DETAILS = Pattern.compile("(?<"+HW_TYPE+">\\w+) \\((?<"+TITLE+">[\\w\\s\\d]+)\\) has (?<"+NUM_AXES+">\\d+) axes \\((?<"+AXIS_NAMES+">[\\w\\s,]+)\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern DESCRIPTION = Pattern.compile("and (?<"+NUM_BUTTONS+">\\d+) buttons \\((?<"+BUTTON_NAMES+">[\\w\\s,]+)\\)\\.", Pattern.CASE_INSENSITIVE);

    private final Process process;
    private final Consumer<JSTestEvent> sink;
    private final JSMetadata metadata;

    public JSTestReader(Path joystickPath, ExecutorService pool, Consumer<JSTestEvent> sink) throws IOException {
        this.metadata = new JSMetadata(joystickPath);
        this.process = new ProcessBuilder(
                "jstest",
                "--event",
                joystickPath.toAbsolutePath().toString())
            .start();
        this.sink = sink;

        pool.submit(() -> ProcessUtil.linesInputStream(process.getInputStream())
                .filter(this::interceptMetadata)
                .map(joystickEventString -> new JSTestEvent(metadata, joystickEventString))
                .forEach(this.sink));
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

    private static JSTestReader constructUnchecked(Path joystickPath, ExecutorService pool, Consumer<JSTestEvent> sink) {
        try {
            return new JSTestReader(joystickPath, pool, sink);
        } catch (IOException e) {
            throw new JSTestException(e);
        }
    }

    public static List<JSTestReader> getJoysticks(ExecutorService pool, Consumer<JSTestEvent> sink) {
        return getJoysticksPaths()
                .stream()
                .map(path -> JSTestReader.constructUnchecked(path, pool, sink))
                .collect(Collectors.toList());
    }


    @Override
    public void close() throws Exception {
        process.destroy();
    }

    public boolean isDead() {
        return !process.isAlive();
    }

    public boolean interceptMetadata(String event) {
        if (!JSTestEvent.validEvent(event)) {
            Matcher detailsMatcher = DETAILS.matcher(event);
            Matcher descriptionMatcher = DESCRIPTION.matcher(event);
            if (detailsMatcher.matches()) {
                metadata.setTitle(detailsMatcher.group(TITLE));
                JSMetadata.index(detailsMatcher.group(AXIS_NAMES), metadata.getAxisNames()::put);

            }
            if (descriptionMatcher.matches()) {
                JSMetadata.index(descriptionMatcher.group(BUTTON_NAMES), metadata.getButtonNames()::put);
            }
            return false;
        }
        return true;
    }
}
