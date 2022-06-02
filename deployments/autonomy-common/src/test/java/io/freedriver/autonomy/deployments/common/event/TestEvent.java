package io.freedriver.autonomy.deployments.common.event;

import java.util.Objects;
import java.util.UUID;

public class TestEvent {
    private UUID uuid;

    public TestEvent() {
        this(UUID.randomUUID());
    }

    public TestEvent(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestEvent testEvent = (TestEvent) o;
        return Objects.equals(uuid, testEvent.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }
}
