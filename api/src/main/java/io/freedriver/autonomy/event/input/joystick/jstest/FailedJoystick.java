package io.freedriver.autonomy.event.input.joystick.jstest;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import static java.time.temporal.ChronoUnit.MINUTES;

/**
 * Convenience class to keep track of joystick devices that failed to spawn readers.
 */
public class FailedJoystick {
    private static final Duration DEFAULT_DURATION = Duration.of(5, MINUTES);

    private final Path path;
    private final Instant instant;

    public FailedJoystick(Path path, Instant instant) {
        this.path = path;
        this.instant = instant;
    }

    public FailedJoystick(Path path) {
        this(path, Instant.now().plus(DEFAULT_DURATION));
    }

    public Path getPath() {
        return path;
    }

    public Instant getInstant() {
        return instant;
    }

    public boolean failureExpired() {
        return Instant.now().isAfter(instant);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FailedJoystick that = (FailedJoystick) o;
        return Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }

    public Duration getDelay() {
        return Duration.between(Instant.now(), getInstant())
                .abs();
    }
}
