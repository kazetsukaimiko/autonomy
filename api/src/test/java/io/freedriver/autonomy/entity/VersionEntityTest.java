package io.freedriver.autonomy.entity;

import io.freedriver.autonomy.entity.jsonlink.VersionEntity;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public class VersionEntityTest {

    @Test
    public void testVersionDefault() {
        assertEquals("1.0.0", new VersionEntity().toString());
    }

    @Test
    public void testBump() {
        testVersionModifiers(new VersionEntity(), VersionEntity::bump, "1.0.1");
        testVersionModifiers(new VersionEntity().milestone(), VersionEntity::bump, "1.1.1");
        testVersionModifiers(new VersionEntity().release(), VersionEntity::bump, "2.0.1");
    }

    @Test
    public void testMilestone() {
        testVersionModifiers(new VersionEntity(), VersionEntity::milestone, "1.1.0");
        testVersionModifiers(new VersionEntity().bump(), VersionEntity::milestone, "1.1.0");
        testVersionModifiers(new VersionEntity().release(), VersionEntity::milestone, "2.1.0");
    }

    @Test
    public void testRelease() {
        testVersionModifiers(new VersionEntity(), VersionEntity::release, "2.0.0");
        testVersionModifiers(new VersionEntity().milestone(), VersionEntity::release, "2.0.0");
        testVersionModifiers(new VersionEntity().milestone().bump(), VersionEntity::release, "2.0.0");
        testVersionModifiers(new VersionEntity().release(), VersionEntity::release, "3.0.0");

    }

    protected void testVersionModifiers(VersionEntity victim, Function<VersionEntity, VersionEntity> modifier, String result) {
        VersionEntity modified = modifier.apply(victim);
        assertEquals(result, modified.toString());
        assertNotSame(victim, modified, "Modifiers should create a copy.");
    }

}
