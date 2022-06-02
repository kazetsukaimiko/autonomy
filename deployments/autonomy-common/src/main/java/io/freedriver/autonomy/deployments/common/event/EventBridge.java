package io.freedriver.autonomy.deployments.common.event;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import org.jboss.logging.Logger;

import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public abstract class EventBridge implements Closeable {

    @Inject
    Event<Object> generalEvents;

    private final Logger logger = Logger.getLogger(getClass());
    private final List<ReceivedItem<?>> recentHistory = Collections.synchronizedList(new ArrayList<>());

    protected abstract boolean supportsObject(Object o);
    protected abstract void sendObject(Object o) throws IOException, InterruptedException;

    public void shutdown(@Observes ShutdownEvent shutdownEvent) throws IOException {
        logger.info("Received shutdown event.");
        close();
    }

    /**
     * Intercept all CDI events to pass to the event bridge
     */
    public void bridgeFromCDI(@Observes Object o) throws IOException, InterruptedException {
        if (o instanceof ReceivedItem) {
            logger.warnf("Someone fired a %s<%s>, which is illegal.", ReceivedItem.class.getSimpleName(), ((ReceivedItem<?>) o).getItemClassAsString());
            return;
        }
        if (!bannedType(o) && supportsObject(o) && notSomethingIJustReceived(o)) {
            logger.infof("Submitting payload %s of type %s via event bridge.", o, o != null ? o.getClass().getName() : "null");
            sendObject(o);
        }
    }

    private boolean notSomethingIJustReceived(Object o) {
        synchronized (recentHistory) {
            return expireHistory()
                    .stream()
                    .noneMatch(existingItem -> existingItem.hasExactItem(o));
        }
    }

    public boolean bannedType(Object o) {
        return (o instanceof StartupEvent) ||
                (o instanceof ShutdownEvent) ||
                (o instanceof String)
                ;
    }

    /**
     * It is expected that the implementation call this method upon receipt and parsing of a message.
     */
    protected void bridgeToCDI(ReceivedItem<?> receivedItem) {
        synchronized (recentHistory) {
            if (expireHistory()
                    .stream()
                    .noneMatch(existingItem -> existingItem.match(receivedItem))) {
                recentHistory.add(receivedItem);
                generalEvents.fire(receivedItem.getItem());
            }
        }
    }

    private List<ReceivedItem<?>> expireHistory() {
        recentHistory.removeIf(ReceivedItem::isExpired);
        return recentHistory;
    }



    /*
    Test purpose: Is CDI event bus polymorphic/type based?
    Will submitting to a general Event<?> bus push the events out to their strict typed endpoints? -> TRUE
     */

    /*
    public void testAtStart(@Observes StartupEvent startupEvent) {
        submitEvent(() -> "String Event");
        submitEvent(UUID::randomUUID);
    }

    public void submitEvent(@Observes @Any EventMessage event) {
        generalEvents.fire(event.getContent());
    }

    public void consumeObject(@Observes Object objectEvent) {
        System.out.println("Observing objectEvent " + objectEvent);
    }

    public void consumeString(@Observes String stringEvent) {
        System.out.println("Observing stringEvent " + stringEvent);
    }

    public void consumeUUID(@Observes UUID uuidEvent) {
        System.out.println("Observing uuidEvent " + uuidEvent);
    }

     */

}
