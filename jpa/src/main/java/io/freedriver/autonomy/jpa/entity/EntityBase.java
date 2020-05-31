package io.freedriver.autonomy.jpa.entity;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import java.util.Objects;

@MappedSuperclass
public abstract class EntityBase extends EmbeddedEntityBase {
    @Id
    @GeneratedValue
    private long id;

    public EntityBase() {
    }

    public EntityBase(EntityBase entityBase) {
        this.id = entityBase.id;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EntityBase that = (EntityBase) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "EntityBase{" +
                "id=" + id +
                '}';
    }
}
