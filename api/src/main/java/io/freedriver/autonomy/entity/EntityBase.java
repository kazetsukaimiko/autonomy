package io.freedriver.autonomy.entity;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.freedriver.autonomy.iface.Positional;
import org.dizitart.no2.NitriteId;
import org.dizitart.no2.objects.Id;

import java.util.Objects;

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@entity")
public abstract class EntityBase implements Positional {
    @Id
    private NitriteId id;
    private long position = 0;

    public EntityBase() {
    }

    public NitriteId getId() {
        return id;
    }

    public void setId(NitriteId id) {
        this.id = id;
    }

    @Override
    public long getPosition() {
        return position;
    }

    @Override
    public void setPosition(long position) {
        this.position = position;
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
                ", position=" + position +
                '}';
    }
}
