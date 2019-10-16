package io.freedriver.autonomy.hrorm.mock;

import java.util.function.Function;

public class ColumnConfiguration<ENTITY, FIELD> {
    private final Class<ENTITY> interfaceClass;
    private final Class<FIELD> fieldClass;
    private final String columnName;
    private final Function<ENTITY, FIELD> getter;

    public ColumnConfiguration(Class<ENTITY> interfaceClass, Class<FIELD> fieldClass, String columnName, Function<ENTITY, FIELD> getter) {
        this.interfaceClass = interfaceClass;
        this.fieldClass = fieldClass;
        this.columnName = columnName;
        this.getter = getter;
    }

    public Class<ENTITY> getInterfaceClass() {
        return interfaceClass;
    }

    public Class<FIELD> getFieldClass() {
        return fieldClass;
    }

    public String getColumnName() {
        return columnName;
    }

    public Function<ENTITY, FIELD> getGetter() {
        return getter;
    }
}
