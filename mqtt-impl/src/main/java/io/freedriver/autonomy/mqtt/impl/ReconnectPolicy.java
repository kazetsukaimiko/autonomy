package io.freedriver.autonomy.mqtt.impl;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Capped exponential backoff. Delay is {@code min(cap, initial * 2^(attempt-1))},
 * then reduced by up to {@code jitterFraction} of that value.
 */
public final class ReconnectPolicy {

    private final Duration initial;
    private final Duration cap;
    private final double jitterFraction;
    private final java.util.function.DoubleSupplier unitRandom;

    public ReconnectPolicy(Duration initial, Duration cap, double jitterFraction,
            java.util.function.DoubleSupplier unitRandom) {
        if (initial == null || initial.isZero() || initial.isNegative()) {
            throw new IllegalArgumentException("initial");
        }
        if (cap == null || cap.compareTo(initial) < 0) {
            throw new IllegalArgumentException("cap");
        }
        if (jitterFraction < 0 || jitterFraction >= 1) {
            throw new IllegalArgumentException("jitterFraction");
        }
        this.initial = initial;
        this.cap = cap;
        this.jitterFraction = jitterFraction;
        this.unitRandom = unitRandom;
    }

    public static ReconnectPolicy standard() {
        return new ReconnectPolicy(Duration.ofSeconds(1), Duration.ofSeconds(60), 0.2,
                () -> ThreadLocalRandom.current().nextDouble());
    }

    public Duration delay(int attempt) {
        if (attempt < 1) {
            throw new IllegalArgumentException("attempt");
        }
        long millis = initial.toMillis();
        long capMillis = cap.toMillis();
        for (int step = 1; step < attempt && millis < capMillis; step++) {
            millis = Math.min(capMillis, millis * 2);
        }
        long span = (long) Math.floor(millis * jitterFraction);
        double roll = unitRandom.getAsDouble();
        if (roll < 0 || roll > 1) {
            roll = 0;
        }
        long reduction = span <= 0 ? 0 : (long) Math.floor(span * roll);
        return Duration.ofMillis(Math.max(1, millis - reduction));
    }
}
