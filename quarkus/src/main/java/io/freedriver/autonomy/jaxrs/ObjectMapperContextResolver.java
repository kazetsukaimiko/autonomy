package io.freedriver.autonomy.jaxrs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.freedriver.autonomy.vedirect.jackson.VEDirectModule;
import io.freedriver.jsonlink.jackson.JsonLinkModule;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ObjectMapperContextResolver implements ContextResolver<ObjectMapper> {

    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JsonLinkModule())
            .registerModule(new VEDirectModule())
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
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