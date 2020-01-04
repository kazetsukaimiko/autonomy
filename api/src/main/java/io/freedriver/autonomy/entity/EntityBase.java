package io.freedriver.autonomy.entity;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.dizitart.no2.NitriteId;
import org.dizitart.no2.objects.Id;

import java.util.Objects;

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@entity")
public abstract class EntityBase extends EmbeddedEntityBase {
    @Id
    private NitriteId id;

    public EntityBase() {
    }


    public EntityBase(EntityBase entityBase) {
        this.id = entityBase.id;
    }

    public NitriteId getId() {
        return id;
    }

    public void setId(NitriteId id) {
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
                "nitriteId=" + id +
                '}';
    }
}
