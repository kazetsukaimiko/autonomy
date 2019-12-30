package io.freedriver.autonomy.rest.provider;

import io.freedriver.autonomy.rest.converter.NitriteIdConverter;
import org.dizitart.no2.NitriteId;

import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

@Provider
public class NitriteIdConverterProvider implements ParamConverterProvider {
    private final NitriteIdConverter converter = new NitriteIdConverter();

    @Override
    public <T> ParamConverter<T> getConverter(Class<T> aClass, Type type, Annotation[] annotations) {
        return !aClass.equals(NitriteId.class) ? null : (ParamConverter<T>) converter;
    }
}
