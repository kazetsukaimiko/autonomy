package io.freedriver.autonomy.cdi.qualifier;


import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import javax.enterprise.util.Nonbinding;
import javax.inject.Qualifier;

import io.freedriver.victron.VictronProduct;

@Qualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface VEProduct {
    @Nonbinding
    VictronProduct value();
    @Nonbinding
    String serial();
}
