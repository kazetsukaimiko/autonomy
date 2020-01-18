package io.freedriver.autonomy.service.crud;

import io.freedriver.autonomy.ee.Autonomy;
import io.freedriver.autonomy.entity.EntityBase;
import io.freedriver.autonomy.entity.jsonlink.WorkspaceEntity;
import io.freedriver.autonomy.iface.Positional;
import io.freedriver.ee.cdi.qualifier.NitriteDatabase;
import org.dizitart.no2.FindOptions;
import org.dizitart.no2.Nitrite;
import org.dizitart.no2.NitriteId;
import org.dizitart.no2.SortOrder;
import org.dizitart.no2.WriteResult;
import org.dizitart.no2.objects.Cursor;
import org.dizitart.no2.objects.ObjectFilter;
import org.dizitart.no2.objects.ObjectRepository;
import org.dizitart.no2.objects.filters.ObjectFilters;

import javax.inject.Inject;
import javax.validation.constraints.Null;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public abstract class NitriteCRUDService<T extends EntityBase> implements CRUDInterface<NitriteId, T> {
    private final Logger logger = Logger.getLogger(getKlazz().getName());

    @Inject
    @NitriteDatabase(deployment = Autonomy.DEPLOYMENT, database = WorkspaceEntity.class)
    private Nitrite nitrite;

    protected NitriteCRUDService() {

    }

    public Logger getLogger() {
        return logger;
    }
    public Nitrite getNitrite() {
        return nitrite;
    }

    public void setNitrite(Nitrite nitrite) {
        this.nitrite = nitrite;
    }

    protected abstract Class<T> getKlazz();

    @Override
    public long count() {
        return findAll()
                .count();
    }

    public ObjectRepository<EntityBase> getEntityRepository() {
        return getNitrite()
                .getRepository(EntityBase.class);
    }


    public Stream<T> findAll(ObjectFilter filter, FindOptions options) {
        getLogger().log(Level.INFO, "Getting entity " + getKlazz().getName() + " with find options and filter");
        return streamCursor(getEntityRepository().find(filter, options));
    }

    public Stream<T> findAll(FindOptions options) {
        getLogger().log(Level.INFO, "Getting entity " + getKlazz().getName() + " with find options");
        return streamCursor(getEntityRepository().find(options));
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
    public Optional<T> findOne(NitriteId nitriteId) {
        getLogger().log(Level.INFO, "Getting entity " + getKlazz().getName() + " by id " + nitriteId.toString());
        return findAllIds(Stream.of(nitriteId))
                .findFirst();
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
        WriteResult writeResult = getEntityRepository().update(entity);
        long affected = streamWriteResult(writeResult).count();
        getLogger().log(Level.INFO, "Updated " + affected + " entities");
        return entity;
    }

    public List<T> saveOrder(List<T> entities) {
        getLogger().log(Level.INFO, "Saving entity order for " + getKlazz().getName());
        return Positional.reorder(entities)
                .stream()
                .map(this::save)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<NitriteId> delete(T entity) {
        try {
            return Optional.of(entity)
                    .map(getEntityRepository()::remove)
                    .map(WriteResult::iterator)
                    .filter(Iterator::hasNext)
                    .map(Iterator::next);
        }catch (NullPointerException npe) {
            logger.log(Level.SEVERE, "Nitrite BUG: NPE On Delete Iterator", npe);
            return Optional.ofNullable(entity)
                    .map(EntityBase::getId);
        }
    }

    @Override
    public Stream<NitriteId> deleteAll(Stream<T> entities) {
        return entities.map(this::delete).flatMap(Optional::stream);
    }

    @Override
    public Optional<NitriteId> deleteById(NitriteId id) {
        logger.info("Deleting id: " + String.valueOf(id));
        try {
            return Optional.of(id)
                    .map(this::nitriteIdFilter)
                    .map(getEntityRepository()::remove)
                    .map(WriteResult::iterator)
                    .filter(Iterator::hasNext)
                    .map(Iterator::next);
        } catch (NullPointerException npe) {
            logger.log(Level.SEVERE, "Nitrite BUG: NPE On Delete Iterator", npe);
            return Optional.ofNullable(id);
        }
    }

    private ObjectFilter nitriteIdFilter(NitriteId nitriteId) {
        return ObjectFilters.eq("id", nitriteId);
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

    public static ObjectFilter byBoardId(UUID boardId) {
        return ObjectFilters.eq("boardId", boardId);
    }

    public Stream<T> findByBoardId(UUID boardId) {
        return findAll(byBoardId(boardId));
    }

    public Optional<T> findOneByBoardId(UUID boardId) {
        return findAll(byBoardId(boardId)).findFirst();
    }

    public static FindOptions sort() {
        return FindOptions.sort("position", SortOrder.Ascending);
    }

    private Stream<NitriteId> streamWriteResult(WriteResult writeResult) {
        return StreamSupport.stream(writeResult.spliterator(), false);
    }

    private <X> Stream<T> streamCursor(Cursor<EntityBase> cursor) {
        return streamCursor(cursor, getKlazz());
    }

    private <X extends EntityBase> Stream<X> streamCursor(Iterable<EntityBase> cursor, Class<X> childClass) {
        return StreamSupport.stream(cursor.spliterator(), false)
                .filter(childClass::isInstance)
                .map(childClass::cast);
    }
}
