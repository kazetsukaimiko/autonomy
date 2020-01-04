package io.freedriver.autonomy.entity.jsonlink;

import org.dizitart.no2.NitriteId;

public class MapEntity extends JsonLinkEntity {
    private NitriteId permutationId;
    private NitriteId pinId;

    public MapEntity() {
    }

    public NitriteId getPermutationId() {
        return permutationId;
    }

    public void setPermutationId(NitriteId permutationId) {
        this.permutationId = permutationId;
    }

    public NitriteId getPinId() {
        return pinId;
    }

    public void setPinId(NitriteId pinId) {
        this.pinId = pinId;
    }

    @Override
    public String toString() {
        return "MapEntity{" +
                "permutationId=" + permutationId +
                ", pinId=" + pinId +
                '}';
    }
}
