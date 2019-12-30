package io.freedriver.autonomy.entity;

import io.freedriver.autonomy.iface.Positional;
import org.dizitart.no2.NitriteId;
import org.dizitart.no2.objects.Id;

import java.util.Objects;

public abstract class EntityBase<T extends EntityBase<T>> implements Positional {
    @Id
    private NitriteId nitriteId;
    private long position = 0;

    public EntityBase() {
    }

    public NitriteId getNitriteId() {
        return nitriteId;
    }

    public void setNitriteId(NitriteId nitriteId) {
        this.nitriteId = nitriteId;
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
        EntityBase<?> that = (EntityBase<?>) o;
        return Objects.equals(nitriteId, that.nitriteId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nitriteId);
    }

    @Override
    public String toString() {
        return "EntityBase{" +
                "nitriteId=" + nitriteId +
                ", position=" + position +
                '}';
    }
}
