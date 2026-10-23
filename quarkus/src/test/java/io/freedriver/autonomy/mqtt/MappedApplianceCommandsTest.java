package io.freedriver.autonomy.mqtt;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import io.freedriver.autonomy.service.SimpleAliasService;
import io.freedriver.jsonlink.config.v2.Appliance;
import io.freedriver.jsonlink.config.v2.Mapping;
import io.freedriver.jsonlink.config.v2.Mappings;
import io.freedriver.jsonlink.jackson.schema.v1.Identifier;
import org.junit.jupiter.api.Test;

class MappedApplianceCommandsTest {

    private static final UUID FIRST = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SECOND = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void unknownNameDoesNotTouchABoardAndFridgeUsesTheFirstBoard() throws Exception {
        SimpleAliasService aliases = mock(SimpleAliasService.class);
        Appliance fridge = new Appliance(Identifier.of(40), "fridge", Set.of());
        Appliance laterFridge = new Appliance(Identifier.of(42), "fridge", Set.of());
        Mapping first = new Mapping(FIRST, "first", List.of(fridge), null, null, null);
        Mapping second = new Mapping(SECOND, "second", List.of(laterFridge), null, null, null);
        when(aliases.getMappings()).thenReturn(new Mappings(null, null, Set.of(first, second), Map.of()));

        MappedApplianceCommands commands = new MappedApplianceCommands(
                aliases, new CacheApplianceStateSource(aliases, new ConcurrentHashMap<>()));

        assertFalse(commands.apply("lamp", true));
        assertTrue(commands.apply("fridge", false));

        verify(aliases, never()).setState(eq(SECOND), any());
        verify(aliases, times(1)).setState(FIRST, Map.of("fridge", false));
    }
}
