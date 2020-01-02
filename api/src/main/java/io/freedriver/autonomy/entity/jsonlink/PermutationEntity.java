package io.freedriver.autonomy.entity.jsonlink;

import org.dizitart.no2.NitriteId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PermutationEntity extends JsonLinkEntity  {
    private NitriteId groupId;
    private List<NitriteId> activePins = new ArrayList<>();
    private List<NitriteId> inactivePins = new ArrayList<>();
    private boolean initialState = false;

    public PermutationEntity() {
    }

    public NitriteId getGroupId() {
        return groupId;
    }

    public void setGroupId(NitriteId groupId) {
        this.groupId = groupId;
    }

    public List<NitriteId> getActivePins() {
        return activePins;
    }

    public void setActivePins(List<NitriteId> activePins) {
        this.activePins = activePins;
    }

    public List<NitriteId> getInactivePins() {
        return inactivePins;
    }

    public void setInactivePins(List<NitriteId> inactivePins) {
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
                Objects.equals(groupId, that.groupId) &&
                Objects.equals(activePins, that.activePins);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), groupId, activePins, initialState);
    }

    @Override
    public String toString() {
        return "PermutationEntity{" +
                "groupId=" + groupId +
                ", activePins=" + activePins +
                ", initialState=" + initialState +
                '}';
    }
}
