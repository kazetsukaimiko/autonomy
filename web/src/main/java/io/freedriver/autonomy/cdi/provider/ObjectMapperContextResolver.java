package io.freedriver.autonomy.cdi.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dizitart.no2.mapper.NitriteIdModule;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

@Provider
public class ObjectMapperContextResolver implements ContextResolver<ObjectMapper> {

    private final ObjectMapper mapper;

    public ObjectMapperContextResolver() {
        this.mapper = new ObjectMapper()
            .registerModule(new NitriteIdModule());
    }


    @Override
    public ObjectMapper getContext(Class<?> aClass) {
        return mapper;
    }
}