package io.freedriver.autonomy.cdi.qualifier;

import org.dizitart.no2.Nitrite;

import javax.inject.Qualifier;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.function.BiConsumer;
import java.util.function.Function;

@Qualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface NitriteDatabase {
    Class<?> value() default Object.class;
}
