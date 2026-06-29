package io.freedriver.autonomy.cdi.qualifier;


import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import jakarta.enterprise.util.Nonbinding;
import jakarta.inject.Qualifier;

import io.freedriver.victron.VictronProduct;

@Qualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface VEProduct {
    @Nonbinding
    VictronProduct value();
    @Nonbinding
    String serial();
}
