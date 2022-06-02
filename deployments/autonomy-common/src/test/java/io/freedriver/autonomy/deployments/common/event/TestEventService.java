package io.freedriver.autonomy.deployments.common.event;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import java.util.HashSet;
import java.util.Set;

@ApplicationScoped
public class TestEventService {
    private Set<TestEvent> received = new HashSet<>();
    public void waitForEvent(@Observes TestEvent testEvent) {
        received.add(testEvent);
    }

    public Set<TestEvent> getReceived() {
        return received;
    }
}
