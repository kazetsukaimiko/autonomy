package io.freedriver.autonomy.jpa.entity.event;

import java.time.Instant;

public class RetryLater extends Event {

    public RetryLater() {
    }

    public RetryLater(Instant timestamp, EventCoordinate coordinate, EventDescription description, SourceType sourceType, EventPriority priority) {
        super(timestamp, coordinate, description, sourceType, priority);
    }

    public RetryLater(Instant timestamp, EventCoordinate coordinate, EventDescription description, SourceType sourceType) {
        super(timestamp, coordinate, description, sourceType);
    }
}
