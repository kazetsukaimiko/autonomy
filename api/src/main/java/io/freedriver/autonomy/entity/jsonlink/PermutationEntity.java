package io.freedriver.autonomy.entity.jsonlink;

import io.freedriver.autonomy.entity.EmbeddedEntityBase;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PermutationEntity extends EmbeddedEntityBase {
    private List<PinEntity> activePins = new ArrayList<>();
    private List<PinEntity> inactivePins = new ArrayList<>();
    private boolean initialState = false;

    public PermutationEntity() {
    }

    public PermutationEntity(PermutationEntity entity) {
        super(entity);
        this.activePins = entity.activePins;
        this.inactivePins = entity.inactivePins;
        this.initialState = entity.initialState;
    }

    public List<PinEntity> getActivePins() {
        return activePins;
    }

    public void setActivePins(List<PinEntity> activePins) {
        this.activePins = activePins;
    }

    public List<PinEntity> getInactivePins() {
        return inactivePins;
    }

    public void setInactivePins(List<PinEntity> inactivePins) {
        this.inactivePins = inactivePins;
    }

    public boolean isInitialState() {
        return initialState;
    }

    public void setInitialState(boolean initialState) {
        this.initialState = initialState;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        PermutationEntity that = (PermutationEntity) o;
        return initialState == that.initialState &&
                Objects.equals(activePins, that.activePins);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), activePins, initialState);
    }

    @Override
    public String toString() {
        return "PermutationEntity{" +
                ", activePins=" + activePins +
                ", initialState=" + initialState +
                '}';
    }
}
