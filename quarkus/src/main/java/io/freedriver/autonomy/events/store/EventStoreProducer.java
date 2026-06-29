package io.freedriver.autonomy.events.store;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class EventStoreProducer {

    @Produces
    @ApplicationScoped
    public EventStore eventStore(MongoEventStore mongoEventStore) {
        return mongoEventStore;
    }
}