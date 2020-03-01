package io.freedriver.autonomy.rest;

import io.freedriver.autonomy.ee.Autonomy;
import io.freedriver.autonomy.entity.event.Event;
import io.freedriver.ee.cdi.qualifier.NitriteDatabase;
import org.dizitart.no2.Nitrite;
import org.dizitart.no2.NitriteId;
import org.dizitart.no2.objects.ObjectRepository;

import javax.inject.Inject;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class NitriteEventApi implements EventApi<NitriteId> {

    @Inject
    @NitriteDatabase(deployment = Autonomy.DEPLOYMENT, database = Event.class)
    private Nitrite nitrite;

    public ObjectRepository<Event> getRepository() {
        return nitrite.getRepository(Event.class);
    }

    @Override
    public NitriteId create(Event event) {
        return StreamSupport.stream(getRepository().insert(event)
                .spliterator(), false)
                .findFirst()
                .orElse(null);
    }

    @Override
    public Stream<Event> findAll() {
        return StreamSupport.stream(getRepository().find().spliterator(), false);
    }

    @Override
    public Event findOne(NitriteId nitriteId) {
        return getRepository().getById(nitriteId);
    }
}
