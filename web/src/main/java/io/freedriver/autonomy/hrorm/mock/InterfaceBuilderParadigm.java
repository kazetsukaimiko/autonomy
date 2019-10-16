package io.freedriver.autonomy.hrorm.mock;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;

public class InterfaceBuilderParadigm<ENTITY> {
    private static final Logger LOGGER = Logger.getLogger(InterfaceBuilderParadigm.class.getName());

    private final Class<ENTITY> interfaceClass;
    private final List<ColumnConfiguration<ENTITY, ?>> columns = new ArrayList<>();

    public InterfaceBuilderParadigm(Class<ENTITY> interfaceClass) {
        if (!interfaceClass.isInterface()) {
            throw new IllegalArgumentException("Immutable class must be an interface");
        }
        this.interfaceClass = interfaceClass;
    }

    public static void demo() {
        InterfaceBuilderParadigm<Person> personDaoBuilder = new InterfaceBuilderParadigm<>(Person.class)
                .withIntegerColumn("id", Person::getId)
                .withStringColumn("name", Person::getName)
                .withIntegerColumn("age", Person::getAge)
                .withStringColumn("email", Person::getEmailAddress);

        personDaoBuilder.testBuild();
    }

    public InterfaceBuilderParadigm<ENTITY> withStringColumn(String name, Function<ENTITY, String> getter) {
        columns.add(new ColumnConfiguration<>(interfaceClass, String.class, name, getter));
        return this;
    }

    public InterfaceBuilderParadigm<ENTITY> withIntegerColumn(String name, Function<ENTITY, Long> getter) {
        columns.add(new ColumnConfiguration<>(interfaceClass, Long.class, name, getter));
        return this;
    }

    @SuppressWarnings("unchecked")
    public InterfaceBuilderParadigm<ENTITY> testBuild() {

        ResultInvocationHandler<ENTITY> resultHandler = new ResultInvocationHandler<>(interfaceClass);
        // Detection
        columns.forEach(columnDefinition -> this.showOff(columnDefinition, resultHandler));

        ENTITY entity = (ENTITY) Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class[] {interfaceClass}, resultHandler);

        // Lets use the getters
        columns.forEach(columnDefinition -> this.testGetter(columnDefinition, entity));

        return this;
    }

    private void testGetter(ColumnConfiguration<ENTITY,?> columnDefinition, ENTITY entity) {
        LOGGER.info("Fetching column value \""+columnDefinition.getColumnName()+"\" from " + columnDefinition.getInterfaceClass().getSimpleName());
        LOGGER.info("Found " + String.valueOf(columnDefinition.getGetter().apply(entity)));
    }

    private <FIELD> void showOff(ColumnConfiguration<ENTITY, FIELD> entityColumn, ResultInvocationHandler<ENTITY> handler) {
        Method m = findMethodOfGetterFunction(interfaceClass, entityColumn.getGetter());
        LOGGER.info("Entity column " + entityColumn.getColumnName() + " refers to field/getter " + m.getName() +
                " which returns " + m.getReturnType().toString() + " (expected "+entityColumn.getFieldClass().toString()+")");

        // Now to set an example value.
        if (entityColumn.getFieldClass().equals(String.class)) {
            ColumnConfiguration<ENTITY, String> stringColumn = (ColumnConfiguration<ENTITY, String>) entityColumn;
            if (stringColumn.getColumnName().equals("name")) {
                handler.returnFromGetter(stringColumn.getGetter(), "ojplg");
            } else if (stringColumn.getColumnName().equals("email")) {
                handler.returnFromGetter(stringColumn.getGetter(), "ojplg@hrorm.org");
            }
        }

        if (entityColumn.getFieldClass().equals(Long.class)) {
            ColumnConfiguration<ENTITY, Long> longColumn = (ColumnConfiguration<ENTITY, Long>) entityColumn;
            handler.returnFromGetter(longColumn.getGetter(), Long.MAX_VALUE);
        }
    }

    @SuppressWarnings("unchecked")
    public static <ENTITY, FIELD> Method findMethodOfGetterFunction(final Class<ENTITY> interfaceClass, Function<ENTITY, FIELD> getter) {
        if (!interfaceClass.isInterface()) {
            throw new IllegalArgumentException("Immutable class must be an interface");
        }
        ENTITY proxy = (ENTITY) Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class[] {interfaceClass}, MethodDetector.INSTANCE);

        try {
            getter.apply(proxy);
        } catch (MethodInvokedException mie) {
            return mie.getMethod();
        }
        return null; // Never happens
    }


    private static class MethodDetector implements InvocationHandler {
        public static final MethodDetector INSTANCE = new MethodDetector();

        private MethodDetector() {
        }

        @Override
        public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
            throw new MethodInvokedException(method);
        }
    }

    private static class ResultInvocationHandler<ENTITY> implements InvocationHandler {
        private final Class<ENTITY> interfaceClass;
        private final Map<Method, Object> values = new HashMap<>();

        public ResultInvocationHandler(Class<ENTITY> interfaceClass) {
            this.interfaceClass = interfaceClass;
        }

        @Override
        public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
            return values.get(method);
        }

        public <FIELD> ResultInvocationHandler<ENTITY> returnFromGetter(Function<ENTITY, FIELD> getter, FIELD value) {
            values.put(findMethodOfGetterFunction(interfaceClass, getter), value);
            return this;
        }

    }

}
