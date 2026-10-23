package io.freedriver.autonomy.mqtt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import io.freedriver.autonomy.cdi.qualifier.ConnectorCache;
import io.freedriver.autonomy.service.PinCoordinate;
import io.freedriver.autonomy.service.SimpleAliasService;
import io.freedriver.jsonlink.config.v2.Appliance;
import io.freedriver.jsonlink.config.v2.Mapping;
import io.freedriver.jsonlink.config.v2.Mappings;
import io.freedriver.jsonlink.jackson.schema.v1.Identifier;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Names come from the mappings file. On/off comes from the connector cache.
 * The wire is keyed by appliance name: boards are ordered by connector UUID,
 * and the first appliance with a name is kept. Later copies are dropped.
 * Pins the cache does not have yet are left out rather than reported as off.
 */
@ApplicationScoped
public class CacheApplianceStateSource implements ApplianceStateSource {

    private static final Logger LOG = LoggerFactory.getLogger(MqttLog.CATEGORY);

    private final SimpleAliasService aliases;
    private final Map<PinCoordinate, Boolean> cache;
    private final Set<String> loggedDuplicates = new HashSet<>();

    @Inject
    public CacheApplianceStateSource(SimpleAliasService aliases,
            @ConnectorCache Map<PinCoordinate, Boolean> cache) {
        this.aliases = aliases;
        this.cache = cache;
    }

    @Override
    public Snapshot read() throws IOException {
        List<Placed> appliances = mapped();
        Map<String, Boolean> states = new LinkedHashMap<>();
        for (Placed appliance : appliances) {
            if (appliance.state() != null) {
                states.put(appliance.name(), appliance.state());
            }
        }
        return new Snapshot(states, appliances.size());
    }

    /**
     * Board for the appliance name the state publish keeps. Boards are ordered
     * by connector UUID, and the first listing of a name wins. Empty when the
     * mappings do not contain that name.
     */
    public Optional<UUID> boardFor(String applianceName) throws IOException {
        if (applianceName == null || applianceName.isBlank()) {
            return Optional.empty();
        }
        for (Placed appliance : mapped()) {
            if (applianceName.equals(appliance.name())) {
                return Optional.of(appliance.boardId());
            }
        }
        return Optional.empty();
    }

    private List<Placed> mapped() throws IOException {
        Mappings mappings = aliases.getMappings();
        List<Mapping> boards = new ArrayList<>(mappings.getMappings());
        boards.sort(Comparator.comparing(Mapping::connectorId, Comparator.nullsLast(Comparator.naturalOrder())));
        List<Placed> placed = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Mapping board : boards) {
            UUID boardId = board.connectorId();
            if (boardId == null || board.appliances() == null) {
                continue;
            }
            for (Appliance appliance : board.appliances()) {
                String name = appliance.name();
                Identifier pin = appliance.identifier();
                if (name == null || name.isBlank() || pin == null) {
                    continue;
                }
                if (!seen.add(name)) {
                    logDuplicate(boardId, name);
                    continue;
                }
                placed.add(new Placed(boardId, name, cache.get(new PinCoordinate(boardId, pin))));
            }
        }
        return List.copyOf(placed);
    }

    private record Placed(UUID boardId, String name, Boolean state) {
    }

    private void logDuplicate(UUID boardId, String name) {
        if (loggedDuplicates.add(boardId + "|" + name)) {
            LOG.warn("duplicate appliance name '{}' on board {}; keeping the earlier board's state", name, boardId);
        }
    }
}
