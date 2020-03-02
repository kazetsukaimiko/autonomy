package io.freedriver.jsonlink;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import static java.time.temporal.ChronoUnit.MINUTES;

public class FailedConnector {
    private static final Duration DEFAULT_DURATION = Duration.of(5, MINUTES);

    private final String device;
    private final Instant instant;

    public FailedConnector(String device, Instant instant) {
        this.device = device;
        this.instant = instant;
    }

    public FailedConnector(String device) {
        this(device, Instant.now().plus(DEFAULT_DURATION));
    }

    public String getDevice() {
        return device;
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
        FailedConnector that = (FailedConnector) o;
        return Objects.equals(device, that.device);
    }

    @Override
    public int hashCode() {
        return Objects.hash(device);
    }

    public Duration getDelay() {
        return Duration.between(Instant.now(), getInstant())
                .abs();
    }
}
