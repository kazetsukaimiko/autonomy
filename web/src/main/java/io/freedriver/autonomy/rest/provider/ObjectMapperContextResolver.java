package io.freedriver.autonomy.rest.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.freedriver.jsonlink.jackson.JsonLinkModule;
import org.dizitart.no2.mapper.NitriteIdModule;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

@Provider
public class ObjectMapperContextResolver implements ContextResolver<ObjectMapper> {

    private final ObjectMapper mapper;

    public ObjectMapperContextResolver() {
        this.mapper = new ObjectMapper()
            .registerModule(new JsonLinkModule())
            .registerModule(new NitriteIdModule());
    }


    @Override
    public ObjectMapper getContext(Class<?> aClass) {
        return mapper;
    }
}