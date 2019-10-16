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

    /*
     * Add java.sql.Connection, obviously.
     */
    public InterfaceBuilderParadigm(Class<ENTITY> interfaceClass) {
        if (!interfaceClass.isInterface()) {
            throw new IllegalArgumentException("Immutable class must be an interface");
        }
        this.interfaceClass = interfaceClass;
    }

    public static void demo() {
        /*
         * So building the dao is similar, but only requires getter methods. For convertingXColumn, you'll need
         * at least a Function<X, FIELD>, but not a Converter<X, FIELD>.
         */
        InterfaceBuilderParadigm<Person> personDaoBuilder = new InterfaceBuilderParadigm<>(Person.class)
                .withIntegerColumn("id", Person::getId)
                .withStringColumn("name", Person::getName)
                .withIntegerColumn("age", Person::getAge)
                .withStringColumn("email", Person::getEmailAddress);

        personDaoBuilder.testBuild();
    }

    /*
     * Store all the information about the columns obtained.
     */
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

        /*
         * The ResultInvocationHandler contains all the field values, and you make one per row.
         * Its job is to recreate the interface as the user defined it.
         * If the user fails to define a column for a method, throw UnsupportedOperationException.
         */
        ResultInvocationHandler<ENTITY> resultHandler = new ResultInvocationHandler<>(interfaceClass);

        columns.forEach(columnDefinition -> this.populateColumnData(columnDefinition, resultHandler));

        /*
         * This method makes resultHandler masquerade as an ENTITY. All methods that get called against this instance
         * are instead forwarded to invoke() on the InvocationHandler class passed.
         */
        ENTITY entity = (ENTITY) Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class[] {interfaceClass}, resultHandler);

        /*
         * Testing getters.
         */
        columns.forEach(columnDefinition -> this.testGetter(columnDefinition, entity));

        return this;
    }

    private void testGetter(ColumnConfiguration<ENTITY,?> columnDefinition, ENTITY entity) {
        LOGGER.info("Fetching column value \""+columnDefinition.getColumnName()+"\" from " + columnDefinition.getInterfaceClass().getSimpleName());
        LOGGER.info("Found " + String.valueOf(columnDefinition.getGetter().apply(entity)));
    }

    /*
     * We're going to setup the return values for the handler here based on the entity column.
     * Note that while the generics upstream were erased, they're present here.
     */
    private <FIELD> void populateColumnData(ColumnConfiguration<ENTITY, FIELD> entityColumn, ResultInvocationHandler<ENTITY> handler) {

        // Not needed here- just for logging.
        Method m = findMethodOfGetterFunction(interfaceClass, entityColumn.getGetter());
        LOGGER.info("Entity column " + entityColumn.getColumnName() + " refers to field/getter " + m.getName() +
                " which returns " + m.getReturnType().toString() + " (expected "+entityColumn.getFieldClass().toString()+")");

        /*
         * Now to set an example value. This is where you'd take an actual column value and pass it into handler.
         * In hrorm's use case I doubt the need to cast like this. ColumnDefinition would contain the needed methods to extract
         * the desired column data from ResultSet and you could just call
         * handler.returnFromGetter(entityColumn, entityColumn.extract(resultSet)
         * or something similar.
         *
         * Because I have to populate with static values here, I do the conditional cast.
         */

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

    /*
     * We use an exception to capture the method invoked by the getter. java.lang.reflect.Method actually has a hashCode implementation
     * we can reasonably use here- getters don't take any arguments.
     *
     * You could make these interface classes behave like mutables by defining a setter as well, but why would we want to do that? =)
     */
    @SuppressWarnings("unchecked")
    public static <ENTITY, FIELD> Method findMethodOfGetterFunction(final Class<ENTITY> interfaceClass, Function<ENTITY, FIELD> getter) {
        if (!interfaceClass.isInterface()) {
            throw new IllegalArgumentException("Immutable class must be an interface");
        }
        ENTITY proxy = (ENTITY) Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class[] {interfaceClass}, MethodDetector.INSTANCE);

        try {
            // Trigger the exception
            getter.apply(proxy);
        } catch (MethodInvokedException mie) {
            return mie.getMethod();
        }
        return null; // Never happens.
    }

    /*
     * The Invocation handler that detects what java.lang.reflect.Method is invoked by a getter/method reference (which
     * themselves have no metadata to say "who they belong to", etc.
     */
    private static class MethodDetector implements InvocationHandler {
        public static final MethodDetector INSTANCE = new MethodDetector();

        private MethodDetector() {
        }

        @Override
        public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
            // Exceptions are used because getter.apply(entity) can only return FIELD. Even though this is a proxy, Java
            // will still attempt to cast to the return type of the method once it gets back to the masqueraded ENTITY type.
            // Exceptions can contain any information we want to pass out of a method that otherwise has a restricted return type.
            throw new MethodInvokedException(method);
        }
    }

    /*
     * This InvocationHandler's job is to contain the values from a Row in a ResultSet, and return them when the appropriate
     * method gets called. It should throw UnsupportedOperationException or a descendant if the implementer calls a method
     * they didn't define as a ColumnConfiguration.
     */
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
