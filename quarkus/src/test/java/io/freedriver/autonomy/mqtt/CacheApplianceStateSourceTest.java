package io.freedriver.autonomy.mqtt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import io.freedriver.autonomy.service.PinCoordinate;
import io.freedriver.autonomy.service.SimpleAliasService;
import io.freedriver.jsonlink.config.v2.Appliance;
import io.freedriver.jsonlink.config.v2.Mapping;
import io.freedriver.jsonlink.config.v2.Mappings;
import io.freedriver.jsonlink.jackson.schema.v1.Identifier;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

class CacheApplianceStateSourceTest {

    private static final UUID FIRST = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SECOND = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void readsNamesFromMappingsAndOnOffFromTheCache() throws Exception {
        SimpleAliasService aliases = mock(SimpleAliasService.class);
        Map<PinCoordinate, Boolean> cache = new ConcurrentHashMap<>();
        cache.put(new PinCoordinate(FIRST, Identifier.of(40)), true);
        cache.put(new PinCoordinate(SECOND, Identifier.of(42)), false);
        cache.put(new PinCoordinate(FIRST, Identifier.of(41)), false);
        when(aliases.getMappings()).thenReturn(mappings());

        CacheApplianceStateSource source = new CacheApplianceStateSource(aliases, cache);
        ApplianceStateSource.Snapshot snapshot = source.read();

        assertEquals(Map.of("fridge", true, "hallway", false), snapshot.states());
        assertEquals(2, snapshot.mappedCount());
        verify(aliases).getMappings();
        verify(aliases, never()).currentState(ArgumentMatchers.any(), ArgumentMatchers.any());
        verify(aliases, never()).makeView(ArgumentMatchers.any());
        assertFalse(Files.readString(sourceFile()).contains("currentState"));
        assertFalse(Files.readString(sourceFile()).contains("makeView"));
        assertFalse(Files.readString(sourceFile()).contains("ConnectorService"));
        assertFalse(Files.readString(sourceFile()).contains("writeDigital"));
        assertFalse(Files.readString(sourceFile()).contains("readDigital"));
    }

    @Test
    void duplicateNamesKeepTheEarliestBoardAndUncachedPinsAreOmitted() throws Exception {
        SimpleAliasService aliases = mock(SimpleAliasService.class);
        when(aliases.getMappings()).thenReturn(mappings());
        Map<PinCoordinate, Boolean> cache = new ConcurrentHashMap<>();
        cache.put(new PinCoordinate(FIRST, Identifier.of(40)), true);

        ApplianceStateSource.Snapshot snapshot = new CacheApplianceStateSource(aliases, cache).read();

        assertEquals(Map.of("fridge", true), snapshot.states());
        assertTrue(snapshot.states().get("fridge"));
        assertEquals(2, snapshot.mappedCount());
        assertFalse(snapshot.states().containsKey("hallway"));
    }

    @Test
    void sameBoardKeepsTheFirstListingOfAName() throws Exception {
        UUID board = FIRST;
        Appliance first = new Appliance(Identifier.of(1), "fridge", Set.of());
        Appliance second = new Appliance(Identifier.of(2), "fridge", Set.of());
        Mapping mapping = new Mapping(board, "board", List.of(first, second), null, null, null);
        Mappings mappings = new Mappings(null, null, Set.of(mapping));
        SimpleAliasService aliases = mock(SimpleAliasService.class);
        when(aliases.getMappings()).thenReturn(mappings);
        Map<PinCoordinate, Boolean> cache = new ConcurrentHashMap<>();
        cache.put(new PinCoordinate(board, Identifier.of(1)), false);
        cache.put(new PinCoordinate(board, Identifier.of(2)), true);

        ApplianceStateSource.Snapshot snapshot = new CacheApplianceStateSource(aliases, cache).read();

        assertEquals(Map.of("fridge", false), snapshot.states());
        assertEquals(1, snapshot.mappedCount());
    }

    @Test
    void mappingReadFailuresPropagate() throws Exception {
        SimpleAliasService aliases = mock(SimpleAliasService.class);
        when(aliases.getMappings()).thenThrow(new IOException("unreadable"));
        CacheApplianceStateSource source = new CacheApplianceStateSource(aliases, new ConcurrentHashMap<>());
        assertThrows(IOException.class, source::read);
    }

    private static Mappings mappings() {
        Appliance fridge = new Appliance(Identifier.of(40), "fridge", Set.of());
        Appliance hallway = new Appliance(Identifier.of(41), "hallway", Set.of());
        Appliance otherFridge = new Appliance(Identifier.of(42), "fridge", Set.of());
        Mapping first = new Mapping(FIRST, "first", List.of(fridge, hallway), null, null, null);
        Mapping second = new Mapping(SECOND, "second", List.of(otherFridge), null, null, null);
        return new Mappings(null, null, Set.of(first, second));
    }

    private static Path sourceFile() {
        return Path.of("src/main/java/io/freedriver/autonomy/mqtt/CacheApplianceStateSource.java");
    }
}
