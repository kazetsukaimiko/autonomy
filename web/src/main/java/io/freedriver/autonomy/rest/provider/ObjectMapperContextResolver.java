package io.freedriver.autonomy.rest.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.freedriver.jsonlink.jackson.JsonLinkModule;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

@Provider
public class ObjectMapperContextResolver implements ContextResolver<ObjectMapper> {

    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JsonLinkModule());
    public ObjectMapperContextResolver() {
    }

    public static ObjectMapper getMapper() {
        return mapper;
    }

    @Override
    public ObjectMapper getContext(Class<?> aClass) {
        return getMapper();
    }
}