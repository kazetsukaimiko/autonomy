package io.freedriver.autonomy.service.crud;

import io.freedriver.autonomy.entity.EntityBase;
import io.freedriver.autonomy.iface.Positional;
import org.dizitart.no2.NitriteId;

import javax.enterprise.context.Dependent;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;

@Dependent
public class CRUDCache {
    private final Map<EntityBaseCacheKey, EntityBase> cacheMap = new ConcurrentHashMap<>();

    public <E extends EntityBase> NitriteId delete(E entity, Function<E, NitriteId> deleteFunction) {
        cacheMap.remove(new EntityBaseCacheKey(entity));
        return deleteFunction.apply(entity);
    }

    public <E extends EntityBase> E one(Class<E> klazz, NitriteId id, Function<NitriteId, E> fetchByIdFunction) {
        return fetch(klazz, id)
                .orElseGet(() -> push(fetchByIdFunction.apply(id)));
    }

    public <E extends EntityBase> Stream<E> all(Class<E> klazz, Stream<NitriteId> ids, Function<NitriteId, E> fetchByIdFunction) {
        return ids.map(id -> one(klazz, id, fetchByIdFunction));
    }

    public <E extends EntityBase> Stream<E> cache(Stream<E> inputStream) {
        return inputStream.map(this::push);
    }

    public <E extends EntityBase> E push(E entity) {
        cacheMap.put(new EntityBaseCacheKey(entity), entity);
        return entity;
    }

    private <E extends EntityBase> Optional<E> fetch(Class<E> klazz,  NitriteId id) {
        return Optional.of(new EntityBaseCacheKey(klazz, id))
                .flatMap(this::cacheMapGet)
                .filter(klazz::isInstance)
                .map(klazz::cast);
    }

    private Optional<EntityBase> cacheMapGet(EntityBaseCacheKey cacheKey) {
        cacheMap.keySet().stream()
                .filter(EntityBaseCacheKey::expired)
                .forEach(cacheMap::remove);
        return Optional.of(cacheKey)
                .map(cacheMap::get);
    }
}
