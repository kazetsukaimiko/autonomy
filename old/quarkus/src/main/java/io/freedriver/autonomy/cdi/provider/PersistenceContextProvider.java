package io.freedriver.autonomy.cdi.provider;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PersistenceContextProvider {
    /*

    private final Map<String, EntityManagerFactory> byUnitName = new LinkedHashMap<>();

    @Produces @Default
    public EntityManager getEntityManager(InjectionPoint injectionPoint) {
        EnergyConverter energyConverter = new EnergyConverter();
        //ValidationException
        PersistenceContext persistenceContext = injectionPoint.getAnnotated()
                .getAnnotation(PersistenceContext.class);
        if (persistenceContext != null) {

            return byUnitName.computeIfAbsent(persistenceContext.unitName(), unitName ->
                    this.makeEntityManagerByUnitName(unitName, persistenceContext.properties()))
                    .createEntityManager();
        } else {
            throw new IllegalStateException("Injection point " + injectionPoint.toString() + " contains no persistence context");
        }
    }

    private EntityManagerFactory makeEntityManagerByUnitName(String unitName, PersistenceProperty[] properties) {
        Map<String, String> map = new LinkedHashMap<>();
        Stream.of(properties)
                .forEach(property -> map.put(property.name(), property.value()));

        map.put("javax.persistence.provider", HibernatePersistenceProvider.class.getName());


        return Persistence.createEntityManagerFactory(unitName, map);
    }


    /*
    @Produces @Default
    public DataSource makeDataSource() {
        DataSource dataSource = new MariaDbDataSource()
    }*/


        /*
    static {
        try (ScanResult scanResult = new ClassGraph()
        .verbose()
        .enableClassInfo()
        //.enableAllInfo()
        .acceptPackages("io.freedriver")
        .scan()) {
            DRIVER_CLASSES = scanResult.getAllClasses()
                    .stream()
                    .filter(classInfo -> classInfo.implementsInterface(Driver.class.getName()))
                    .map(ClassInfo::loadClass)
                    .collect(Collectors.toSet());
        }
    }*/

}
