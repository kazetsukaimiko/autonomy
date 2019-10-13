package io.freedriver.autonomy.hrorm;

import io.freedriver.controller.JoystickEvent;
import org.hrorm.Converter;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class PathConverter implements Converter<Path, String> {
    @Override
    public String from(Path path) {
        return Optional.ofNullable(path)
                .map(Path::toAbsolutePath)
                .map(Path::toString)
                .orElse(null);
    }

    @Override
    public Path to(String s) {
        return Optional.ofNullable(s)
                .map(Paths::get)
                .orElse(null);
    }
}
