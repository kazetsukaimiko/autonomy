package io.freedriver.autonomy.service.crud;

import io.freedriver.autonomy.entity.EntityBase;
import org.dizitart.no2.NitriteId;

import java.time.ZonedDateTime;
import java.util.Objects;

public class EntityBaseCacheKey {
    private final Class<? extends EntityBase> entityClass;
    private final NitriteId entityId;
    private final ZonedDateTime expires = ZonedDateTime.now();

    public EntityBaseCacheKey(Class<? extends EntityBase> entityClass, NitriteId entityId) {
        this.entityClass = entityClass;
        this.entityId = entityId;
    }

    public <E extends EntityBase> EntityBaseCacheKey(E entity) {
        this(entity.getClass(), entity.getId());
    }

    public Class<? extends EntityBase> getEntityClass() {
        return entityClass;
    }

    public NitriteId getEntityId() {
        return entityId;
    }

    public ZonedDateTime getExpires() {
        return expires;
    }

    public boolean expired() {
        return ZonedDateTime.now().isAfter(getExpires());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EntityBaseCacheKey that = (EntityBaseCacheKey) o;
        return Objects.equals(entityClass, that.entityClass) &&
                Objects.equals(entityId, that.entityId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entityClass, entityId);
    }
}
