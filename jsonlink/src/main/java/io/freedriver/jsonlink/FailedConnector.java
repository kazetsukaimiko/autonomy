package io.freedriver.jsonlink;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.SECONDS;

public class FailedConnector {
    private static final Duration DEFAULT_DURATION = Duration.of(5, MINUTES);
    private static final Duration TIMEOUT_DURATION = Duration.of(30, SECONDS);

    private final String device;
    private final Instant instant;

    public FailedConnector(String device, Instant instant) {
        this.device = device;
        this.instant = instant;
    }

    public static FailedConnector failed(String device) {
        return new FailedConnector(device, Instant.now().plus(DEFAULT_DURATION));
    }

    public static FailedConnector timedOut(String device) {
        return new FailedConnector(device, Instant.now().plus(TIMEOUT_DURATION));
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
