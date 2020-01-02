package io.freedriver.autonomy.service.crud;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.freedriver.autonomy.BaseTest;
import io.freedriver.autonomy.entity.EntityBase;
import io.freedriver.autonomy.entity.jsonlink.PinNameEntity;
import org.dizitart.no2.Nitrite;
import org.dizitart.no2.objects.ObjectRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

public abstract class NitriteCRUDServiceUnitTest<T extends EntityBase, C extends NitriteCRUDService<T>> extends BaseTest {
    abstract Class<T> getKlazz();
    abstract Function<Nitrite, C> getConstructor();
    abstract T create(int idx);

    private Path nitritePath;
    private Nitrite nitrite;
    private C victim;

    @BeforeEach
    public void init() {
        nitritePath = Paths.get("/tmp", UUID.randomUUID().toString());
        nitrite = Nitrite.builder()
                .registerModule(new JavaTimeModule())
                .compressed()
                .filePath(nitritePath.toFile())
                .openOrCreate("nitrite", "nitrite");
        victim = getConstructor().apply(nitrite);
    }

    @AfterEach
    public void destroy() throws IOException {
        victim = null;
        nitrite.commit();
        nitrite.close();
        Files.deleteIfExists(nitritePath);
    }

    @Test
    public void testFinders() {
        List<T> entities = makeEntities(10);

        System.out.println(victim.getEntityRepository().getDocumentCollection().size());
        victim.getEntityRepository()
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

    protected C getVictim() {
        return victim;
    }
}
