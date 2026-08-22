package io.freedriver.autonomy.event.input.joystick.jstest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.freedriver.autonomy.event.input.joystick.jstest.parser.JSMetadataParser;
import io.freedriver.autonomy.event.input.joystick.jstest.parser.JSTestEventParser;
import io.freedriver.base.util.ProcessUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JSTestReader {

    private JSTestReader() {
    }

    /**
     * Take a stream of input lines, and transform them into JSTestEvents. Accumulate JSMetadata
     * as header lines appear; event lines capture the current metadata.
     */
    public static Stream<JSTestEvent> readEvents(Stream<String> rawLines) {
        JSMetadata[] current = { JSMetadata.empty() };
        return rawLines.flatMap(rawLine -> addOrMakeMetadata(current, rawLine));
    }

    public static Stream<JSTestEvent> ofJoystick(Path joystickPath) {
        final Process p = jstestProcess(joystickPath);
        return readEvents(ProcessUtil.linesInputStream(p.getInputStream()).onClose(() -> destroyProcess(p, joystickPath)));
    }

    public static void destroyProcess(Process p, Path joystickPath) {
        log.info("Destroying Joystick {}", joystickPath.toAbsolutePath());
        p.destroyForcibly();
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

    private static Stream<JSTestEvent> addOrMakeMetadata(JSMetadata[] current, String line) {
        Optional<JSMetadata> updated = JSMetadataParser.INSTANCE.apply(current[0], line);
        if (updated.isPresent()) {
            current[0] = updated.get();
            return Stream.empty();
        }
        return Stream.of(new JSTestEventParser(current[0]).apply(line));
    }
}
