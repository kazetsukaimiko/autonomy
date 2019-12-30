package io.freedriver.autonomy.rest.converter;

import org.dizitart.no2.NitriteId;

import javax.ws.rs.ext.ParamConverter;

public class NitriteIdConverter implements ParamConverter<NitriteId> {
    @Override
    public NitriteId fromString(String s) {
        return NitriteId.createId(Long.parseLong(s));
    }

    @Override
    public String toString(NitriteId nitriteId) {
        return nitriteId.getIdValue().toString();
    }
}
