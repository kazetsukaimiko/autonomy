package io.freedriver.autonomy.deployments.joystick.cache;

import javax.inject.Qualifier;
import java.lang.annotation.*;

@Qualifier
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ConnectorCache {
}
