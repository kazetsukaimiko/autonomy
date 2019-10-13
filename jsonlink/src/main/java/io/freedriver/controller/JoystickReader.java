package io.freedriver.controller;

import io.freedriver.util.ProcessUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JoystickReader implements AutoCloseable {
    private final Path path;
    private final Process process;
    private final Consumer<JoystickEvent> sink;

    public JoystickReader(Path joystickPath, Consumer<JoystickEvent> sink) throws IOException {
        this(joystickPath, Executors.newSingleThreadExecutor(), sink);
    }

    public JoystickReader(Path joystickPath, ExecutorService pool, Consumer<JoystickEvent> sink) throws IOException {
        this.path = joystickPath;
        this.process = new ProcessBuilder(
                "jstest",
                "--event",
                joystickPath.toAbsolutePath().toString())
            .start();
        this.sink = sink;

        pool.submit(() -> ProcessUtil.linesInputStream(process.getInputStream())
                .filter(JoystickEvent::validEvent)
                .map(joystickEventString -> new JoystickEvent(joystickPath, joystickEventString))
                .forEach(this.sink));
    }

    public Path getPath() {
        return path;
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

    private static JoystickReader constructUnchecked(Path joystickPath, Consumer<JoystickEvent> sink) {
        try {
            return new JoystickReader(joystickPath, sink);
        } catch (IOException e) {
            throw new JoystickReaderException(e);
        }
    }

    public static List<JoystickReader> getJoysticks(Consumer<JoystickEvent> sink) {
        return getJoysticksPaths()
                .stream()
                .map(path -> JoystickReader.constructUnchecked(path, sink))
                .collect(Collectors.toList());
    }


    @Override
    public void close() throws Exception {
        process.destroy();
    }

    public boolean isDead() {
        return !process.isAlive();
    }
}
