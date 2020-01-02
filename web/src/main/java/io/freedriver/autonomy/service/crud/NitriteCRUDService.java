package io.freedriver.autonomy.service.crud;

import io.freedriver.autonomy.entity.EntityBase;
import io.freedriver.autonomy.iface.Positional;
import org.dizitart.no2.FindOptions;
import org.dizitart.no2.Nitrite;
import org.dizitart.no2.NitriteId;
import org.dizitart.no2.SortOrder;
import org.dizitart.no2.WriteResult;
import org.dizitart.no2.objects.Cursor;
import org.dizitart.no2.objects.ObjectFilter;
import org.dizitart.no2.objects.ObjectRepository;
import org.dizitart.no2.objects.filters.ObjectFilters;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public abstract class NitriteCRUDService<T extends EntityBase> implements CRUDInterface<NitriteId, T> {
    private Logger logger = Logger.getLogger(getKlazz().getName());

    private final Nitrite nitrite;

    protected NitriteCRUDService(Nitrite nitrite) {
        this.nitrite = nitrite;
    }



    public Logger getLogger() {
        return logger;
    }
    public Nitrite getNitrite() {
        return nitrite;
    }




    abstract Class<T> getKlazz();

    /*
    public ObjectRepository<T> getRepository() {
        return getRepository(getKlazz());
    }


    private <X> ObjectRepository<X> getRepository(Class<X> klazz) {

    }


     */

    public ObjectRepository<EntityBase> getEntityRepository() {
        return getNitrite()
                .getRepository(EntityBase.class);
    }


    public Stream<T> findAll(ObjectFilter filter, FindOptions options) {
        getLogger().log(Level.INFO, "Getting entity " + getKlazz().getName() + " with find options and filter");
        return streamCursor(getEntityRepository().find(filter, options), getKlazz());
    }

    public Stream<T> findAll(FindOptions options) {
        getLogger().log(Level.INFO, "Getting entity " + getKlazz().getName() + " with find options");
        return streamCursor(getEntityRepository().find(options), getKlazz());
    }

    public Stream<T> findAll(ObjectFilter filter) {
        getLogger().log(Level.INFO, "Getting entity " + getKlazz().getName() + " by object filter");
        return findAll(filter, sort());
    }

    @Override
    public NitriteId insert(T entity) {
        WriteResult writeResult = getEntityRepository().insert(entity);
        getNitrite().commit();
        return entity.getId();
    }

    public T save(T entity) {
        if (entity.getId() == null) {
            getLogger().log(Level.INFO, "Inserting entity " + getKlazz().getName() + ": " + String.valueOf(entity));
            insert(entity);
        } else {
            getLogger().log(Level.INFO, "Updating entity " + getKlazz().getName() + ": " + String.valueOf(entity));
            update(entity);
        }
        getNitrite().commit();
        return entity;
    }

    @Override
    public T findOne(NitriteId nitriteId) {
        getLogger().log(Level.INFO, "Getting entity " + getKlazz().getName() + " by id " + nitriteId.toString());
        return findAllIds(Stream.of(nitriteId))
                .findFirst()
                .orElse(null);
    }

    @Override
    public Stream<T> findAllIds(Stream<NitriteId> ids) {
        return findAll(ObjectFilters.in("id", ids.toArray()));
    }

    @Override
    public Stream<T> findAll() {
        return findAll(sort());
    }

    @Override
    public T update(T entity) {
        return findOne(getEntityRepository().update(entity).iterator().next());
    }

    public Stream<T> saveOrder(List<T> entities) {
        getLogger().log(Level.INFO, "Saving entity order for " + getKlazz().getName());
        return Positional.reorder(entities)
                .stream()
                .map(this::save);
    }

    @Override
    public NitriteId delete(T entity) {
        return getEntityRepository().remove(entity).iterator().next();
    }

    @Override
    public Stream<NitriteId> deleteAll(Stream<T> entities) {
        return entities.map(this::delete);
    }

    @Override
    public Optional<NitriteId> deleteById(NitriteId id) {
        return Optional.of(id)
                .map(this::nitriteIdFilter)
                .map(getEntityRepository()::remove)
                .map(WriteResult::iterator)
                .map(Iterator::next);
    }

    private ObjectFilter nitriteIdFilter(NitriteId nitriteId) {
        return ObjectFilters.eq("nitriteId", nitriteId);
    }

    @Override
    public Stream<NitriteId> deleteAllById(Stream<NitriteId> ids) {
        return ids.map(this::deleteById)
                .flatMap(Optional::stream);
    }


    public void deleteAll() {
        getLogger().log(Level.INFO, "Deleting all entities");
        getEntityRepository()
                .drop();
    }

    public static FindOptions sort() {
        return FindOptions.sort("position", SortOrder.Ascending);
    }

    private static <X> Stream<X> streamCursor(Cursor<X> cursor) {
        return StreamSupport.stream(cursor.spliterator(), false);
    }

    private static <X extends EntityBase> Stream<X> streamCursor(Cursor<EntityBase> cursor, Class<X> childClass) {
        return streamCursor(cursor)
                .filter(childClass::isInstance)
                .map(childClass::cast);
    }
}
