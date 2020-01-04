package io.freedriver.autonomy;

import io.freedriver.autonomy.entity.EntityBase;
import io.freedriver.autonomy.service.crud.NitriteCRUDService;
import io.freedriver.ee.prop.DeploymentProperties;
import io.freedriver.jsonlink.Connector;
import io.freedriver.jsonlink.Connectors;
import org.jboss.arquillian.container.test.api.BeforeDeployment;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public abstract class NitriteCRUDServiceITTest<T extends EntityBase, CRUD extends NitriteCRUDService<T>> extends BaseITTest {
    abstract CRUD getVictim();
    abstract T generate(UUID boardId, int i);

    @BeforeDeployment
    public void assignDeployment() {
        DeploymentProperties.NAME.assign("autonomy-test");
    }


    List<T> context = new ArrayList<>();

    @Before
    public void init() {

        UUID validBoard = Connectors.allConnectors()
                .map(Connector::getUUID)
                .findFirst()
                .orElseGet(UUID::randomUUID);

        context = IntStream.range(0, 10)
                .mapToObj(i -> this.generate(validBoard, i))
                .map(getVictim()::save)
                .collect(Collectors.toList());

        assertEquals(context.size(), getVictim().findAll().count(),
                "Context must clear each test!");
    }


    @Test
    public void testFind() {
        IntStream.range(0, 10).forEach(i -> {
            T rand = randomElementFrom(context);
            assertTrue(getVictim().findAll()
                    .peek(System.out::println)
                    .anyMatch(t -> Objects.equals(rand, t)));
        });
    }




    /*
    public Stream<T> find();

    public Stream<T> find(ObjectFilter filter, FindOptions options);
    public Stream<T> find(FindOptions options);

    public Stream<T> find(ObjectFilter filter);

    public Stream<T> getAllById(NitriteId... nitriteIds);
    public T getById(NitriteId nitriteId);

    public T getById(WriteResult writeResult);

    public T save(T entity);

    public Stream<T> saveOrder(List<T> entities);

    public void deleteAll();

    public static FindOptions sort();
    */
}
