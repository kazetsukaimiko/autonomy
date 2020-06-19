package io.freedriver.autonomy.cdi.qualifier;


import io.freedriver.victron.VictronProduct;

import javax.enterprise.util.Nonbinding;
import javax.inject.Qualifier;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Qualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface VEProduct {
    @Nonbinding
    VictronProduct value();
    @Nonbinding
    String serial();
}
