package io.freedriver.autonomy.jpa.entity.event;

public class RetryLater extends Event {

    public RetryLater() {
    }

    public RetryLater(long timestamp, EventCoordinate coordinate, EventDescription description, SourceType sourceType, EventPriority priority) {
        super(timestamp, coordinate, description, sourceType, priority);
    }

    public RetryLater(long timestamp, EventCoordinate coordinate, EventDescription description, SourceType sourceType) {
        super(timestamp, coordinate, description, sourceType);
    }
}
