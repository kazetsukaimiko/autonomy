package io.freedriver.autonomy.vedirect;

import kaze.victron.VEDirectMessage;
import org.dizitart.no2.NitriteId;
import org.dizitart.no2.objects.Id;

import java.util.Objects;

public class NitriteVEDirectMessage extends VEDirectMessage {
    @Id
    private NitriteId nitriteId;

    public NitriteVEDirectMessage() {
    }

    public NitriteVEDirectMessage(VEDirectMessage veDirectMessage) {
        super(veDirectMessage);
    }

    public NitriteId getNitriteId() {
        return nitriteId;
    }

    public void setNitriteId(NitriteId nitriteId) {
        this.nitriteId = nitriteId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        NitriteVEDirectMessage that = (NitriteVEDirectMessage) o;
        return Objects.equals(nitriteId, that.nitriteId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), nitriteId);
    }
}
