package io.freedriver.autonomy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import io.freedriver.autonomy.service.SimpleAliasService;
import io.freedriver.autonomy.service.crud.TTLEnforcementService;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(MappingsTestResource.class)
class MappingsFileIsolationTest {

    @Inject
    SimpleAliasService aliases;

    @Inject
    TTLEnforcementService ttl;

    @Test
    void runningAppReadsTheTemporaryMappingsFile() throws Exception {
        TestMappings.assertDefaultUntouched();
        Path configured = Path.of(ConfigProvider.getConfig().getValue(TestMappings.CONFIG_KEY, String.class))
                .toAbsolutePath()
                .normalize();
        Path defaults = TestMappings.defaultMappingsFile().toAbsolutePath().normalize();
        assertNotEquals(defaults, configured);
        assertEquals(TestMappings.EMPTY_MAPPINGS, Files.readString(configured));
        assertEquals(7, aliases.getMappings().getEventTTL().intValue());
        assertTrue(aliases.getMappings().getMappings().isEmpty());
        assertEquals(7, ttl.getMappings().getEventTTL().intValue());

        String sentinel = """
                {"eventTTL":11,"eventTTLUnit":"DAYS","mappings":[]}
                """;
        try {
            Files.writeString(configured, sentinel);
            assertEquals(11, aliases.getMappings().getEventTTL().intValue());
            assertEquals(11, ttl.getMappings().getEventTTL().intValue());
        } finally {
            Files.writeString(configured, TestMappings.EMPTY_MAPPINGS);
        }
        TestMappings.assertDefaultUntouched();
    }
}
