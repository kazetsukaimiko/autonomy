package io.freedriver.autonomy.service.crud;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.freedriver.autonomy.BaseTest;
import io.freedriver.autonomy.entity.EntityBase;
import org.dizitart.no2.Nitrite;
import org.dizitart.no2.objects.ObjectRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.*;

public abstract class NitriteCRUDServiceUnitTest<T extends EntityBase, C extends NitriteCRUDService<T>> extends BaseTest {
    protected static final UUID boardId = UUID.randomUUID();

    abstract Class<T> getKlazz();
    abstract Function<Nitrite, C> getConstructor();
    abstract T create(int idx);

    private Path nitritePath;
    private Nitrite nitrite;

    protected void beforeEach() {
    }

    @BeforeEach
    public void init() {
        nitritePath = Paths.get("/tmp", UUID.randomUUID().toString());
        nitrite = Nitrite.builder()
                .registerModule(new JavaTimeModule())
                .compressed()
                .filePath(nitritePath.toFile())
                .openOrCreate("nitrite", "nitrite");

        getVictim().deleteAll();
        assertEquals(0L, getVictim().count(),
                "Precount has to be 0");

        beforeEach();
    }

    @AfterEach
    public void destroy() throws IOException {
        nitrite.commit();
        nitrite.close();
        Files.deleteIfExists(nitritePath);
    }


    @Test
    public void testFindByBoard() {
        List<T> entities = makeEntities(10);

        List<T> fromDatabase = getVictim().findByBoardId(boardId)
                .collect(Collectors.toList());
        assertEquals(entities.size(), fromDatabase.size(),
                "findAll() size must match created entities");

        LongStream.range(0, entities.size()-1).forEach(idx -> {
            assertEquals(entities.get((int) idx), fromDatabase.get((int) idx));
        });
    }


    @Test
    public void testDeleteAllById() {
        List<T> entities = makeEntities(10);
        assertEquals(entities.size(), getVictim().count(),
                "Counts should match");

        List<T> deletedEntities = entities.stream()
                .filter(e -> RANDOM.nextBoolean())
                .collect(Collectors.toList());

        long deleted = getVictim().deleteAll(deletedEntities.stream())
                .count();
        assertEquals(deleted, deletedEntities.size());

        assertTrue(getVictim().findAll()
                .noneMatch(deletedEntities::contains),
                "No deleted entities should be findable.");

        entities.removeAll(deletedEntities);

        getVictim().deleteAllById(entities.stream().map(EntityBase::getId));

        assertEquals(0L, getVictim().count(),
                "No entities should be left.");
    }

    @Test
    public void testDeleteAll() {
        List<T> entities = makeEntities(10);
        assertEquals(entities.size(), getVictim().count(),
                "Counts should match");

        getVictim().deleteAll();

        assertEquals(0L, getVictim().count(),
                "All entities should delete upon deleteAll.");

    }

    @Test
    public void testSaveOrder() {
        List<T> entities = makeEntities(10);
        IntStream.range(0, 5).forEach(idx -> {
            T entity = randomElementFrom(entities);
            entities.remove(entity);
            entities.add(entity);
        });
        getVictim().saveOrder(entities);

        List<T> fromDatabase = getVictim().findAll()
                .collect(Collectors.toList());

        assertEquals(entities.size(), fromDatabase.size(),
                "findAll() size must match created entities");

        LongStream.range(0, entities.size()-1).forEach(idx -> {
            System.out.println(idx + " vs " + fromDatabase.get((int) idx).getPosition());
            assertEquals(entities.get((int) idx), fromDatabase.get((int) idx));
            assertEquals(idx, fromDatabase.get((int) idx).getPosition());
        });

    }

    @Test
    public void testFinders() {
        List<T> entities = makeEntities(10);

        System.out.println(getVictim().getEntityRepository().getDocumentCollection().size());
        getVictim().getEntityRepository()
                .getDocumentCollection()
                .find()
                .forEach(doc -> System.out.println(doc.toString()));

        getVictim().findAll().forEach(entity -> assertTrue(entities.contains(entity)));

        entities.forEach(entity -> {
            assertEquals(entity, getVictim().findOne(entity.getId()), "Object equivalence");
            getVictim().delete(entity);
            assertNull(getVictim().findOne(entity.getId()));
        });
        /*

         */

    }





    /*
     * HELPERS
     */

    private <X> void split(List<X> in, Consumer<List<X>> one, Consumer<List<X>> two) {
        X x = randomElementFrom(in);
        one.accept(Collections.singletonList(x));
        X y = randomElementFrom(in);
        while (Objects.equals(x, y)) {
            y = randomElementFrom(in);
        }
        two.accept(Collections.singletonList(y));
    }

    protected List<T> makeEntities(int count) {
        return IntStream.range(0, count)
                .mapToObj(this::create)
                .map(getVictim()::save)
                .collect(Collectors.toList());
    }

    protected Nitrite getNitrite() {
        return nitrite;
    }

    protected ObjectRepository<T> getRepository() {
        return getNitrite().getRepository(getKlazz());
    }

    protected <E extends EntityBase, X extends NitriteCRUDService<E>> X getVictim(Function<Nitrite, X> constructor) {
        return constructor.apply(nitrite);
    }

    protected <E extends EntityBase, X extends NitriteCRUDService<E>> E randomEntity(X crud) {
        return randomElementFrom(crud.findAll());
    }

    protected C getVictim() {
        return getVictim(getConstructor());
    }
}
