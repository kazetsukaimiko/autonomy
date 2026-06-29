package io.freedriver.autonomy.cdi.qualifier;

import java.lang.annotation.*;
import javax.inject.Qualifier;

@Qualifier
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ConnectorCache {
}
