package io.freedriver.autonomy.cdi.qualifier;

import javax.enterprise.util.AnnotationLiteral;

public class NitriteDatabaseLiteral extends AnnotationLiteral<NitriteDatabase> implements NitriteDatabase {
    private final Class<?> value;

    public NitriteDatabaseLiteral() {
        this(Object.class);
    }

    public NitriteDatabaseLiteral(Class<?> value) {
        this.value = value;
    }

    @Override
    public Class<?> value() {
        return null;
    }
}
