package io.freedriver.autonomy.service.crud;

import io.freedriver.autonomy.ee.Autonomy;
import io.freedriver.autonomy.entity.EntityBase;
import io.freedriver.autonomy.entity.jsonlink.JsonLinkEntity;
import io.freedriver.autonomy.iface.Positional;
import io.freedriver.ee.cdi.qualifier.NitriteDatabase;
import org.dizitart.no2.FindOptions;
import org.dizitart.no2.Nitrite;
import org.dizitart.no2.NitriteId;
import org.dizitart.no2.SortOrder;
import org.dizitart.no2.WriteResult;
import org.dizitart.no2.objects.ObjectFilter;
import org.dizitart.no2.objects.ObjectRepository;
import org.dizitart.no2.objects.filters.ObjectFilters;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public abstract class NitriteCRUDService<T extends EntityBase<T>> {
    @Inject
    @NitriteDatabase(deployment = Autonomy.DEPLOYMENT, database = EntityBase.class)
    private Nitrite nitrite;
    public Nitrite getNitrite() {
        return nitrite;
    }

    abstract Class<T> getKlazz();

    public ObjectRepository<T> getRepository() {
        return getNitrite()
                .getRepository(getKlazz());
    }

    public Stream<T> find() {
        return find(sort());
    }

    public Stream<T> find(ObjectFilter filter, FindOptions options) {
        return StreamSupport.stream(getRepository().find(filter, options).spliterator(), false);
    }

    public Stream<T> find(FindOptions options) {
        return StreamSupport.stream(getRepository().find(options).spliterator(), false);
    }

    public Stream<T> find(ObjectFilter filter) {
        return find(filter, sort());
    }

    public Stream<T> getAllById(NitriteId... nitriteIds) {
        return find(ObjectFilters.in("nitriteId", nitriteIds));
    }

    public T getById(NitriteId nitriteId) {
        return getRepository().getById(nitriteId);
    }

    public T getById(WriteResult writeResult) {
        return getById(writeResult.iterator().next());
    }

    @SuppressWarnings("unchecked") // Asinine warning
    public T save(T entity) {
        return getById(entity.getNitriteId() == null ?
                getRepository().insert(entity)
                :
                getRepository().update(entity));
    }

    public Stream<T> saveOrder(List<T> entities) {
        return Positional.reorder(entities)
                .stream()
                .map(this::save);
    }

    public void deleteAll() {
        getRepository()
                .drop();
    }

    public static FindOptions sort() {
        return FindOptions.sort("position", SortOrder.Ascending);
    }
}
