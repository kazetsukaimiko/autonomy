package io.freedriver.autonomy.mqtt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import io.freedriver.autonomy.jaxrs.endpoint.jsonlink.SimpleAliasHandler;
import io.freedriver.autonomy.service.ConnectorService;
import io.freedriver.autonomy.service.PinCoordinate;
import io.freedriver.autonomy.service.SimpleAliasService;
import io.freedriver.jsonlink.config.v2.Appliance;
import io.freedriver.jsonlink.config.v2.Mapping;
import io.freedriver.jsonlink.config.v2.Mappings;
import io.freedriver.jsonlink.jackson.schema.v1.Identifier;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

class GroupSwitchMqttStateTest {

    @Test
    void groupSwitchPublishesTheBoardWithoutAnotherToggle() throws Exception {
        UUID boardId = UUID.fromString("00000000-0000-0000-0000-000000000010");
        Identifier fridgePin = Identifier.of(40);
        Identifier hallwayPin = Identifier.of(41);
        Identifier lampPin = Identifier.of(42);
        Appliance fridge = new Appliance(fridgePin, "fridge", Set.of("kitchen"));
        Appliance hallway = new Appliance(hallwayPin, "hallway", Set.of("lights"));
        Appliance lamp = new Appliance(lampPin, "lamp", Set.of());
        Mapping mapping = new Mapping(boardId, "board", List.of(fridge, hallway, lamp), null, null, null);

        Map<PinCoordinate, Boolean> cache = new ConcurrentHashMap<>();
        cache.put(new PinCoordinate(boardId, fridgePin), false);
        cache.put(new PinCoordinate(boardId, hallwayPin), true);
        cache.put(new PinCoordinate(boardId, lampPin), false);

        ConnectorService connector = mock(ConnectorService.class);
        when(connector.writeDigital(eq(boardId), any())).thenAnswer(invocation -> {
            Map<Identifier, Boolean> requested = invocation.getArgument(1);
            assertEquals(Map.of(fridgePin, true), requested);
            return Map.of(fridgePin, true);
        });

        SimpleAliasService aliases = mock(
                SimpleAliasService.class, withSettings().defaultAnswer(Answers.CALLS_REAL_METHODS));
        setField(aliases, "connectorService", connector);
        setField(aliases, "digitalPinCache", cache);
        doReturn(mapping).when(aliases).getMapping(boardId);
        doReturn(new Mappings(null, null, Set.of(mapping))).when(aliases).getMappings();

        SimpleAliasHandler handler = new SimpleAliasHandler();
        setField(handler, "connectorService", connector);
        setField(handler, "simpleAliasService", aliases);

        handler.setGroup(boardId, "kitchen", true);

        verify(aliases, never()).setState(any(), any());
        verify(connector, never()).send(any(), any());
        verify(connector).writeDigital(eq(boardId), eq(Map.of(fridgePin, true)));

        ApplianceStateSource.Snapshot published = new CacheApplianceStateSource(aliases, cache).read();
        assertEquals(Map.of("fridge", true, "hallway", true, "lamp", false), published.states());
        assertEquals(Boolean.TRUE, cache.get(new PinCoordinate(boardId, hallwayPin)));
        assertEquals(Boolean.FALSE, cache.get(new PinCoordinate(boardId, lampPin)));
    }

    private static void setField(Object target, String name, Object value) throws ReflectiveOperationException {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException notHere) {
                type = type.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }
}
