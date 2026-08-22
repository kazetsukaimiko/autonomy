package io.freedriver.autonomy.event.input.joystick.jstest;

import java.time.Instant;

import lombok.NonNull;

public record JSTestEvent(
        @NonNull JSMetadata metadata,
        @NonNull Instant now,
        @NonNull JSTestEventType jsTestEventType,
        @NonNull Long time,
        @NonNull Long number,
        @NonNull Long value) {}
