package io.freedriver.autonomy;

import io.freedriver.autonomy.entity.EntityBase;
import io.freedriver.autonomy.service.crud.NitriteCRUDService;
import io.freedriver.jsonlink.Connector;
import io.freedriver.jsonlink.Connectors;
import org.jboss.arquillian.container.test.api.BeforeDeployment;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;


public abstract class NitriteCRUDServiceITTest<T extends EntityBase, CRUD extends NitriteCRUDService<T>> extends BaseITTest {
    abstract CRUD getVictim();
    abstract T generate(UUID boardId, int i);

    @BeforeDeployment
    public void assignDeployment() {
        //DeploymentProperties.NAME.assign("autonomy-test"); // TODO
    }

    List<T> context = new ArrayList<>();
    long startCount = 0L;

    @Before
    public void init() {
        UUID validBoard = Connectors.allConnectors()
                .map(Connector::getUUID)
                .findFirst()
                .orElseGet(UUID::randomUUID);

        long initialCount = getVictim().count();

        context = IntStream.range(0, 10)
                .mapToObj(i -> this.generate(validBoard, i))
                .map(getVictim()::save)
                .collect(Collectors.toList());

        startCount = getVictim().count();

        assertEquals(initialCount+context.size(), startCount);
    }



    @After
    public void destroy() {
        context.forEach(getVictim()::delete);
    }


    @Test
    public void testCreate() {
        UUID boardId = UUID.randomUUID();
        long oldItemCount = getVictim().findAll().count();
        long newItemCount = randomNumberOf(500, 100, i -> generate(boardId, i))
                .peek(context::add)
                .map(getVictim()::save)
                .count();

        //assertEquals(newItemCount + oldItemCount, getVictim().findAll().count());
        testFind();
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

    @Test
    public void testDelete() {
        context.stream()
                .peek(getVictim()::delete)
                .forEach(deleted -> {
                    Optional<T> opt = getVictim().findOne(deleted.getId());
                    assertTrue(opt.isEmpty(), "Should no longer be able to find the entity by id");
                    assertTrue(getVictim().findAll()
                            .noneMatch(candidate -> Objects.equals(deleted, candidate)),
                            "No entities should match those deleted");
                });
    }

    @Test
    public void testUpdate() {
        context.forEach(item -> {
            long oldPosition = item.getPosition();

            item.setPosition(randomLong());
            getVictim().save(item);

            Optional<T> newItemOption = getVictim().findOne(item.getId());
            newItemOption.ifPresentOrElse(newItem -> {
                assertNotEquals(oldPosition, newItem.getPosition());
                assertEquals(item.getPosition(), newItem.getPosition());
            }, () -> fail("Cannot find entity"));

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
