package io.freedriver.autonomy.cdi.qualifier;

import kaze.victron.VictronProductType;

import javax.enterprise.util.Nonbinding;
import javax.inject.Qualifier;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Qualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface VEProduct {
    @Nonbinding
    VictronProductType value();
    @Nonbinding
    String serial();
}
